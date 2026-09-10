package com.mamikos.kostapi.credit.service;

import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.entity.CreditTransaction;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.repository.CreditTransactionRepository;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategy;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one page of recharges in its own transaction.
 *
 * <p>This has to be a separate bean, not a method on {@link CreditRechargeService}: Spring's
 * {@code @Transactional(REQUIRES_NEW)} is woven in as a proxy around the bean, and a call
 * from one method to another on {@code this} never passes through that proxy — the
 * "self-invocation" pitfall. Calling this bean's method through its own injected proxy is
 * what actually gets a fresh transaction per batch.
 *
 * <p>The batch is passed in as plain user ids, not the {@code CreditBalance} entities
 * {@link CreditRechargeService} originally read them as: those entities were loaded in a
 * read-only query that commits (and detaches them) before this method's own transaction
 * even opens. Mutating a detached entity does nothing — Hibernate has no session to flush
 * it through — so each wallet is re-fetched here with {@code findByUserIdForUpdate}, inside
 * this transaction, under the same row lock the inquiry-deduction path uses. That lock also
 * protects a real race: a recharge and a concurrent inquiry deduction targeting the same
 * wallet must not interleave, or one of the two updates is silently lost.
 */
@Service
public class CreditRechargeBatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(CreditRechargeBatchExecutor.class);

    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditProperties properties;

    public CreditRechargeBatchExecutor(
            CreditBalanceRepository creditBalanceRepository,
            CreditTransactionRepository creditTransactionRepository,
            CreditProperties properties) {
        this.creditBalanceRepository = creditBalanceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RechargeSummary execute(
            List<Long> userIds,
            RechargeOptions options,
            Map<RechargeStrategyType, RechargeStrategy> strategies,
            Instant now,
            String period) {
        RechargeStrategy strategy = strategies.get(options.strategy());
        ZoneId zone = ZoneId.of(properties.timezone());

        int recharged = 0;
        int skipped = 0;
        for (Long userId : userIds) {
            CreditBalance balance =
                    creditBalanceRepository.findByUserIdForUpdate(userId).orElse(null);
            if (balance == null) {
                // Wallet was deleted between the listing query and this batch running —
                // rare, but not a failure of the recharge itself.
                log.warn("credit.recharge.wallet-missing userId={}", userId);
                skipped++;
                continue;
            }

            if (!options.force() && alreadyRechargedThisMonth(balance, zone, now)) {
                skipped++;
                continue;
            }

            int quota = properties.quotaFor(balance.getUser().getRole());
            int before = balance.getBalance();
            int after = strategy.apply(before, quota, properties.maxBalance());

            if (after == before) {
                skipped++;
                continue;
            }

            if (options.dryRun()) {
                // Deliberately does not touch the entity: mutating it here would still be
                // flushed by Hibernate's dirty checking at commit, silently defeating "dry
                // run".
                recharged++;
                continue;
            }

            balance.applyRecharge(after, now);
            creditBalanceRepository.save(balance);
            creditTransactionRepository.save(
                    CreditTransaction.monthlyRecharge(balance.getUser(), before, after, period));
            recharged++;
        }

        return new RechargeSummary(userIds.size(), recharged, skipped, 0, 0);
    }

    private boolean alreadyRechargedThisMonth(CreditBalance balance, ZoneId zone, Instant now) {
        if (balance.getLastRechargedAt() == null) {
            return false;
        }
        YearMonth lastRecharged = YearMonth.from(balance.getLastRechargedAt().atZone(zone));
        YearMonth current = YearMonth.from(now.atZone(zone));
        return !lastRecharged.isBefore(current);
    }
}
