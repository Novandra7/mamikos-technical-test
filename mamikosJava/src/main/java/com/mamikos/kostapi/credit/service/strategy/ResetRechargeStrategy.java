package com.mamikos.kostapi.credit.service.strategy;

import org.springframework.stereotype.Component;

/**
 * Default strategy (BR-08, decision D-01): the balance is set back to the role's full
 * quota, regardless of what was left. This is what "recharge" means for a subscription
 * quota — it prevents unlimited accumulation by users who rarely spend credit.
 */
@Component
public class ResetRechargeStrategy implements RechargeStrategy {

    @Override
    public RechargeStrategyType type() {
        return RechargeStrategyType.RESET;
    }

    @Override
    public int apply(int currentBalance, int quota, int maxBalance) {
        return quota;
    }
}
