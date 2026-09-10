package com.mamikos.kostapi.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.service.CreditRechargeBatchExecutor;
import com.mamikos.kostapi.credit.service.CreditRechargeService;
import com.mamikos.kostapi.credit.service.RechargeOptions;
import com.mamikos.kostapi.credit.service.RechargeSummary;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import com.mamikos.kostapi.credit.service.strategy.ResetRechargeStrategy;
import com.mamikos.kostapi.user.entity.User;
import com.mamikos.kostapi.user.entity.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

/**
 * Unit-level (Mockito, no database) coverage of the page-by-page loop in
 * {@link CreditRechargeService}. Deliberately does not run this against the shared
 * Testcontainers database the other credit ITs use: "recharge literally everyone" would
 * touch every wallet other tests created too, breaking the isolation every other IT relies
 * on (each of those scopes itself to uniquely-named users). Mocking the repository sidesteps
 * that entirely while still proving the pagination loop keeps requesting pages until
 * {@code Slice#hasNext()} is false.
 */
@ExtendWith(MockitoExtension.class)
class CreditRechargeServiceTest {

    @Mock
    private CreditBalanceRepository creditBalanceRepository;

    private CreditRechargeService service;

    @BeforeEach
    void setUp() {
        CreditProperties.Recharge recharge = new CreditProperties.Recharge("RESET", "0 0 0 1 * *", 1);
        CreditProperties properties = new CreditProperties(new CreditProperties.Quota(20, 40), 5, 200, "UTC", recharge);
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC);

        CreditRechargeBatchExecutor batchExecutor =
                new CreditRechargeBatchExecutorStub(new RechargeSummary(1, 1, 0, 0, 0));

        service = new CreditRechargeService(
                creditBalanceRepository, batchExecutor, properties, clock, List.of(new ResetRechargeStrategy()));
    }

    @Test
    void keepsRequestingPagesUntilTheLastSliceHasNoNext() {
        User firstUser = userWithId(1L);
        User secondUser = userWithId(2L);
        CreditBalance firstPage = CreditBalance.openFor(firstUser, 0);
        CreditBalance secondPage = CreditBalance.openFor(secondUser, 0);

        when(creditBalanceRepository.findAllRechargeable(PageRequest.of(0, 1)))
                .thenReturn(new SliceImpl<>(List.of(firstPage), PageRequest.of(0, 1), true));
        when(creditBalanceRepository.findAllRechargeable(PageRequest.of(1, 1)))
                .thenReturn(new SliceImpl<>(List.of(secondPage), PageRequest.of(1, 1), false));

        RechargeSummary summary =
                service.rechargeAll(new RechargeOptions(RechargeStrategyType.RESET, false, false, null));

        verify(creditBalanceRepository).findAllRechargeable(PageRequest.of(0, 1));
        verify(creditBalanceRepository).findAllRechargeable(PageRequest.of(1, 1));
        // Two pages, each reported by the stub executor as 1 processed/1 recharged.
        assertThat(summary.processed()).isEqualTo(2);
        assertThat(summary.recharged()).isEqualTo(2);
    }

    private User userWithId(Long id) {
        User user = User.builder()
                .name("User " + id)
                .email("user" + id + "@example.test")
                .password("hash")
                .role(UserRole.REGULAR)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** A hand-written stub rather than a Mockito mock: the real method signature takes a
     * live strategy map built by the service itself, which is awkward to match with
     * argument matchers but trivial to just record and answer directly. */
    private static final class CreditRechargeBatchExecutorStub extends CreditRechargeBatchExecutor {

        private final RechargeSummary fixedResult;

        CreditRechargeBatchExecutorStub(RechargeSummary fixedResult) {
            super(null, null, null);
            this.fixedResult = fixedResult;
        }

        @Override
        public RechargeSummary execute(
                List<Long> userIds,
                RechargeOptions options,
                java.util.Map<RechargeStrategyType, com.mamikos.kostapi.credit.service.strategy.RechargeStrategy>
                        strategies,
                Instant now,
                String period) {
            return fixedResult;
        }
    }
}
