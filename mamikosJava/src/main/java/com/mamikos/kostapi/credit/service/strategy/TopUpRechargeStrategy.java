package com.mamikos.kostapi.credit.service.strategy;

import org.springframework.stereotype.Component;

/** Alternative strategy: adds the quota on top of whatever is left, capped at {@code maxBalance}
 * so a user who never spends credit cannot accumulate an unbounded balance. */
@Component
public class TopUpRechargeStrategy implements RechargeStrategy {

    @Override
    public RechargeStrategyType type() {
        return RechargeStrategyType.TOPUP;
    }

    @Override
    public int apply(int currentBalance, int quota, int maxBalance) {
        return Math.min(currentBalance + quota, maxBalance);
    }
}
