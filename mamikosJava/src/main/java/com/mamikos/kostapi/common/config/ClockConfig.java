package com.mamikos.kostapi.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /**
     * Injecting a clock rather than calling {@code Instant.now()} is what makes the
     * month-boundary behaviour of the credit recharge testable without waiting for the
     * first of the month.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
