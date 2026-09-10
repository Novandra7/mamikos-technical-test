package com.mamikos.kostapi.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins, List<String> allowedMethods, List<String> allowedHeaders, long maxAge) {}
