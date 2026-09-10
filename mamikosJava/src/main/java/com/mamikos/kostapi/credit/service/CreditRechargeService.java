package com.mamikos.kostapi.credit.service;

import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategy;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

/**
 * Recharges every regular/premium wallet to (or towards) its role's quota.
 *
 * <p>Runs in pages rather than loading every wallet at once, so a base with hundreds of
 * thousands of users does not exhaust heap in a single pass (PERF-05). Each page is
 * committed by {@link CreditRechargeBatchExecutor} in its own transaction: a failure in one
 * page is logged and does not roll back — or block — pages that already succeeded.
 */
@Service
public class CreditRechargeService {

    private static final Logger log = LoggerFactory.getLogger(CreditRechargeService.class);
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditRechargeBatchExecutor batchExecutor;
    private final CreditProperties properties;
    private final Clock clock;
    private final Map<RechargeStrategyType, RechargeStrategy> strategies;

    public CreditRechargeService(
            CreditBalanceRepository creditBalanceRepository,
            CreditRechargeBatchExecutor batchExecutor,
            CreditProperties properties,
            Clock clock,
            List<RechargeStrategy> strategyBeans) {
        this.creditBalanceRepository = creditBalanceRepository;
        this.batchExecutor = batchExecutor;
        this.properties = properties;
        this.clock = clock;
        this.strategies = strategyBeans.stream().collect(Collectors.toMap(RechargeStrategy::type, Function.identity()));
    }

    public RechargeSummary rechargeAll(RechargeOptions options) {
        Instant start = Instant.now(clock);
        ZoneId zone = ZoneId.of(properties.timezone());
        String period = PERIOD_FORMAT.format(start.atZone(zone));

        RechargeSummary total = options.userId() != null
                ? rechargeSingleUserSafely(options, start, period)
                : rechargeAllPagesSafely(options, start, period);

        long durationMillis = Duration.between(start, Instant.now(clock)).toMillis();
        RechargeSummary withDuration = total.withDuration(durationMillis);
        log.info(
                "credit.recharge.completed processed={} recharged={} skipped={} failed={} durationMs={}",
                withDuration.processed(),
                withDuration.recharged(),
                withDuration.skipped(),
                withDuration.failed(),
                withDuration.durationMillis());
        return withDuration;
    }

    private RechargeSummary rechargeAllPagesSafely(RechargeOptions options, Instant now, String period) {
        RechargeSummary total = RechargeSummary.empty();
        int pageNumber = 0;
        Slice<CreditBalance> slice;
        do {
            Pageable pageable = PageRequest.of(pageNumber, properties.recharge().batchSize());
            slice = creditBalanceRepository.findAllRechargeable(pageable);
            // Only the ids leave this read-only query; the CreditBalance entities themselves
            // become detached the moment it returns (see CreditRechargeBatchExecutor's
            // javadoc), so passing them onward would silently discard every mutation.
            List<Long> userIds =
                    slice.getContent().stream().map(b -> b.getUser().getId()).toList();
            total = total.merge(rechargeBatchSafely(userIds, options, now, period));
            pageNumber++;
        } while (slice.hasNext());
        return total;
    }

    private RechargeSummary rechargeSingleUserSafely(RechargeOptions options, Instant now, String period) {
        if (!creditBalanceRepository.findByUserId(options.userId()).isPresent()) {
            log.warn("credit.recharge.user-not-found userId={}", options.userId());
            return RechargeSummary.empty();
        }
        return rechargeBatchSafely(List.of(options.userId()), options, now, period);
    }

    private RechargeSummary rechargeBatchSafely(
            List<Long> userIds, RechargeOptions options, Instant now, String period) {
        if (userIds.isEmpty()) {
            return RechargeSummary.empty();
        }
        try {
            return batchExecutor.execute(userIds, options, strategies, now, period);
        } catch (RuntimeException failure) {
            // The whole batch rolled back with this exception, so none of its members were
            // actually recharged — count them all as failed rather than guessing.
            log.error("credit.recharge.batch-failed size={}", userIds.size(), failure);
            return new RechargeSummary(userIds.size(), 0, 0, userIds.size(), 0);
        }
    }
}
