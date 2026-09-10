package com.mamikos.kostapi.credit.scheduler;

import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.service.CreditRechargeService;
import com.mamikos.kostapi.credit.service.RechargeOptions;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires the monthly credit recharge (BR-08). {@code @SchedulerLock} keeps exactly one node
 * running it when the API is deployed with more than one instance — without it, every
 * instance's own scheduler would fire at 00:00 and each would recharge every wallet.
 */
@Component
public class CreditRechargeScheduler {

    private final CreditRechargeService creditRechargeService;
    private final CreditProperties properties;

    public CreditRechargeScheduler(CreditRechargeService creditRechargeService, CreditProperties properties) {
        this.creditRechargeService = creditRechargeService;
        this.properties = properties;
    }

    @Scheduled(cron = "${credit.recharge.cron:0 0 0 1 * *}", zone = "${credit.timezone:Asia/Jakarta}")
    @SchedulerLock(name = "creditMonthlyRecharge", lockAtLeastFor = "PT1M", lockAtMostFor = "PT15M")
    public void rechargeMonthly() {
        RechargeStrategyType strategy =
                RechargeStrategyType.valueOf(properties.recharge().strategy());
        creditRechargeService.rechargeAll(RechargeOptions.scheduled(strategy));
    }
}
