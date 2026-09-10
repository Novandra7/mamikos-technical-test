package com.mamikos.kostapi.credit;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.service.CreditRechargeService;
import com.mamikos.kostapi.credit.service.RechargeOptions;
import com.mamikos.kostapi.credit.service.RechargeSummary;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import com.mamikos.kostapi.support.TestApi;
import com.mamikos.kostapi.user.entity.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Exercises the recharge service directly against a real database — not through mocks —
 * because its worst bug was invisible to a mock-based unit test: {@code findAllRechargeable}
 * loads {@code CreditBalance} rows in one (committing, detaching) read-only query, and the
 * batch executor used to mutate those already-detached entities without ever re-attaching
 * them, so the recharge silently did nothing while still reporting success. Only a test that
 * re-reads the balance from a fresh query after the call returns — as this one does — can
 * catch that class of bug.
 */
class CreditRechargeServiceIT extends AbstractIntegrationTest {

    @Autowired
    private CreditRechargeService creditRechargeService;

    @Autowired
    private CreditBalanceRepository creditBalanceRepository;

    @TestConfiguration
    static class FixedClockConfig {
        // A fixed clock lets "already recharged this month" be asserted deterministically,
        // instead of depending on which day the test happens to run.
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void resetStrategyRestoresBalanceToFullQuotaAndIsIdempotentWithinTheSameMonth() {
        AuthResponse regular = TestApi.register(restTemplate, "Recharge User", UserRole.REGULAR);
        Long userId = regular.user().id();
        spendAllCredit(userId);

        assertThat(freshBalance(userId).getBalance()).isZero();

        RechargeSummary first =
                creditRechargeService.rechargeAll(new RechargeOptions(RechargeStrategyType.RESET, false, true, userId));
        assertThat(first.recharged()).isEqualTo(1);

        CreditBalance afterFirstRun = freshBalance(userId);
        assertThat(afterFirstRun.getBalance()).isEqualTo(20);
        assertThat(afterFirstRun.getLastRechargedAt()).isNotNull();

        // Idempotency: running again in the same month (force=false) changes nothing.
        RechargeSummary second = creditRechargeService.rechargeAll(
                new RechargeOptions(RechargeStrategyType.RESET, false, false, userId));
        assertThat(second.recharged()).isZero();
        assertThat(second.skipped()).isEqualTo(1);
        assertThat(freshBalance(userId).getBalance()).isEqualTo(20);
    }

    @Test
    void dryRunReportsWhatWouldHappenWithoutWritingAnything() {
        AuthResponse regular = TestApi.register(restTemplate, "Dry Run User", UserRole.REGULAR);
        Long userId = regular.user().id();
        spendAllCredit(userId);

        RechargeSummary summary =
                creditRechargeService.rechargeAll(new RechargeOptions(RechargeStrategyType.RESET, true, true, userId));

        assertThat(summary.recharged()).isEqualTo(1);
        assertThat(freshBalance(userId).getBalance()).isZero();
        assertThat(freshBalance(userId).getLastRechargedAt()).isNull();
    }

    @Test
    void topUpStrategyAddsQuotaCappedAtMaxBalance() {
        AuthResponse regular = TestApi.register(restTemplate, "TopUp User", UserRole.REGULAR);
        Long userId = regular.user().id();
        // Balance starts at 20; TOPUP should add the 20-credit quota, landing at 40.

        RechargeSummary summary =
                creditRechargeService.rechargeAll(new RechargeOptions(RechargeStrategyType.TOPUP, false, true, userId));

        assertThat(summary.recharged()).isEqualTo(1);
        assertThat(freshBalance(userId).getBalance()).isEqualTo(40);
    }

    @Test
    void ownersAreNeverProcessedBecauseTheyHaveNoWallet() {
        AuthResponse owner = TestApi.register(restTemplate, "Recharge Owner", UserRole.OWNER);

        assertThat(creditBalanceRepository.findByUserId(owner.user().id())).isEmpty();
        // rechargeAll targeting a non-wallet user id must not throw.
        RechargeSummary summary = creditRechargeService.rechargeAll(new RechargeOptions(
                RechargeStrategyType.RESET, false, true, owner.user().id()));
        assertThat(summary.recharged()).isZero();
    }

    private void spendAllCredit(Long userId) {
        // Directly mutate the balance instead of spending it through four HTTP calls: this
        // test is about the recharge service, and the deduction path already has its own
        // dedicated coverage in ConcurrentInquiryIT and AvailabilityInquiryControllerIT.
        CreditBalance balance = creditBalanceRepository.findByUserId(userId).orElseThrow();
        balance.deduct(balance.getBalance());
        creditBalanceRepository.save(balance);
    }

    private CreditBalance freshBalance(Long userId) {
        return creditBalanceRepository.findByUserId(userId).orElseThrow();
    }
}
