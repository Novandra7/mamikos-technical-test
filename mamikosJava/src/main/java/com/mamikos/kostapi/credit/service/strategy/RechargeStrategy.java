package com.mamikos.kostapi.credit.service.strategy;

/** How the monthly recharge turns a current balance and the role's quota into a new one. */
public interface RechargeStrategy {

    RechargeStrategyType type();

    int apply(int currentBalance, int quota, int maxBalance);
}
