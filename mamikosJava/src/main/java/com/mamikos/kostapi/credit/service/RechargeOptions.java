package com.mamikos.kostapi.credit.service;

import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;

/**
 * @param force when true, recharges even a wallet whose {@code lastRechargedAt} already
 *     falls in the current month — used for demos and tests, never by the scheduler
 */
public record RechargeOptions(RechargeStrategyType strategy, boolean dryRun, boolean force, Long userId) {

    public static RechargeOptions scheduled(RechargeStrategyType strategy) {
        return new RechargeOptions(strategy, false, false, null);
    }
}
