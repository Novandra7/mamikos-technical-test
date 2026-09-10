package com.mamikos.kostapi.credit.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import com.mamikos.kostapi.credit.service.strategy.ResetRechargeStrategy;
import org.junit.jupiter.api.Test;

class ResetRechargeStrategyTest {

    private final ResetRechargeStrategy strategy = new ResetRechargeStrategy();

    @Test
    void identifiesAsReset() {
        assertThat(strategy.type()).isEqualTo(RechargeStrategyType.RESET);
    }

    @Test
    void alwaysReturnsTheFullQuotaRegardlessOfCurrentBalance() {
        assertThat(strategy.apply(3, 20, 200)).isEqualTo(20);
        assertThat(strategy.apply(0, 20, 200)).isEqualTo(20);
        assertThat(strategy.apply(50, 20, 200)).isEqualTo(20);
    }
}
