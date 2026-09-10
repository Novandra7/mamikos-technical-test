package com.mamikos.kostapi.credit.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import com.mamikos.kostapi.credit.service.strategy.TopUpRechargeStrategy;
import org.junit.jupiter.api.Test;

class TopUpRechargeStrategyTest {

    private final TopUpRechargeStrategy strategy = new TopUpRechargeStrategy();

    @Test
    void identifiesAsTopUp() {
        assertThat(strategy.type()).isEqualTo(RechargeStrategyType.TOPUP);
    }

    @Test
    void addsTheQuotaOnTopOfTheCurrentBalance() {
        assertThat(strategy.apply(15, 20, 200)).isEqualTo(35);
    }

    @Test
    void neverExceedsTheConfiguredMaxBalance() {
        assertThat(strategy.apply(190, 20, 200)).isEqualTo(200);
    }
}
