package com.mamikos.kostapi.credit.config;

import com.mamikos.kostapi.user.entity.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "credit")
public record CreditProperties(Quota quota, int inquiryCost, int maxBalance, String timezone, Recharge recharge) {

    public record Quota(int regular, int premium) {}

    public record Recharge(String strategy, String cron, int batchSize) {}

    public int quotaFor(UserRole role) {
        return switch (role) {
            case REGULAR -> quota.regular();
            case PREMIUM -> quota.premium();
            case OWNER -> throw new IllegalArgumentException("Owners do not hold a credit wallet");
        };
    }
}
