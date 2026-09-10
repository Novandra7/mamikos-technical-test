package com.mamikos.kostapi.common.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int defaultLimit,
        Duration defaultWindow,
        int authLimit,
        Duration authWindow,
        int inquiryLimit,
        Duration inquiryWindow) {}
