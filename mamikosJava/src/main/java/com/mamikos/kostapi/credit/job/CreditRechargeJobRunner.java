package com.mamikos.kostapi.credit.job;

import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.service.CreditRechargeService;
import com.mamikos.kostapi.credit.service.RechargeOptions;
import com.mamikos.kostapi.credit.service.RechargeSummary;
import com.mamikos.kostapi.credit.service.strategy.RechargeStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Lets an operator trigger the recharge from the command line instead of waiting for the
 * schedule — the same mechanism the install guide uses to demonstrate idempotency:
 *
 * <pre>
 * java -jar mamikos-kost-api.jar --job=credit-recharge --dry-run
 * java -jar mamikos-kost-api.jar --job=credit-recharge --force
 * java -jar mamikos-kost-api.jar --job=credit-recharge   # run again: everything is skipped
 * </pre>
 *
 * <p>When {@code --job} is absent, this runner does nothing and the application starts as
 * a normal web server.
 */
@Component
public class CreditRechargeJobRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CreditRechargeJobRunner.class);
    private static final String JOB_NAME = "credit-recharge";

    private final CreditRechargeService creditRechargeService;
    private final CreditProperties properties;
    private final ConfigurableApplicationContext applicationContext;

    public CreditRechargeJobRunner(
            CreditRechargeService creditRechargeService,
            CreditProperties properties,
            ConfigurableApplicationContext applicationContext) {
        this.creditRechargeService = creditRechargeService;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.getOptionNames().contains("job")
                || !args.getOptionValues("job").contains(JOB_NAME)) {
            return;
        }

        boolean dryRun = args.containsOption("dry-run");
        boolean force = args.containsOption("force");
        RechargeStrategyType strategy = args.containsOption("strategy")
                ? RechargeStrategyType.valueOf(
                        args.getOptionValues("strategy").get(0).toUpperCase(java.util.Locale.ROOT))
                : RechargeStrategyType.valueOf(properties.recharge().strategy());
        Long userId = args.containsOption("user-id")
                ? Long.valueOf(args.getOptionValues("user-id").get(0))
                : null;

        log.info(
                "credit.recharge.manual-trigger strategy={} dryRun={} force={} userId={}",
                strategy,
                dryRun,
                force,
                userId);

        RechargeSummary summary =
                creditRechargeService.rechargeAll(new RechargeOptions(strategy, dryRun, force, userId));

        log.info(
                "credit.recharge.manual-result processed={} recharged={} skipped={} failed={}",
                summary.processed(),
                summary.recharged(),
                summary.skipped(),
                summary.failed());

        int exitCode = summary.failed() > 0 ? 1 : 0;
        System.exit(SpringApplication.exit(applicationContext, () -> exitCode));
    }
}
