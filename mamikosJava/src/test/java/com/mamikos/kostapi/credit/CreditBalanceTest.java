package com.mamikos.kostapi.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mamikos.kostapi.common.exception.InsufficientCreditException;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.user.entity.User;
import com.mamikos.kostapi.user.entity.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure domain-logic unit tests: no Spring context, no database — just the invariant that
 * actually keeps a wallet from going negative. */
class CreditBalanceTest {

    private User regularUser() {
        return User.builder()
                .name("Test")
                .email("test@example.test")
                .password("hash")
                .role(UserRole.REGULAR)
                .build();
    }

    @Test
    void deductingLessThanTheBalanceSucceeds() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 20);

        balance.deduct(5);

        assertThat(balance.getBalance()).isEqualTo(15);
    }

    @Test
    void deductingExactlyTheBalanceLeavesZero() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 5);

        balance.deduct(5);

        assertThat(balance.getBalance()).isZero();
    }

    @Test
    void deductingMoreThanTheBalanceThrowsAndLeavesBalanceUnchanged() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 3);

        assertThatThrownBy(() -> balance.deduct(5)).isInstanceOf(InsufficientCreditException.class);

        assertThat(balance.getBalance()).isEqualTo(3);
    }

    @Test
    void deductingANegativeAmountIsRejected() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 20);

        assertThatThrownBy(() -> balance.deduct(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canAffordReflectsTheCurrentBalance() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 5);

        assertThat(balance.canAfford(5)).isTrue();
        assertThat(balance.canAfford(6)).isFalse();
    }

    @Test
    void applyRechargeSetsBalanceAndTimestamp() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 0);
        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        balance.applyRecharge(20, now);

        assertThat(balance.getBalance()).isEqualTo(20);
        assertThat(balance.getLastRechargedAt()).isEqualTo(now);
    }

    @Test
    void applyRechargeRejectsANegativeTarget() {
        CreditBalance balance = CreditBalance.openFor(regularUser(), 0);

        assertThatThrownBy(() -> balance.applyRecharge(-1, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}
