package com.mamikos.kostapi.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mamikos.kostapi.common.exception.ErrorCode;
import com.mamikos.kostapi.common.ratelimit.RateLimitProperties;
import com.mamikos.kostapi.common.ratelimit.RateLimiter;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Throttles by client IP before authentication runs, so a flood of anonymous requests is
 * rejected without ever touching the database.
 *
 * <p>Login gets a much tighter budget than ordinary traffic because it is the endpoint an
 * attacker would use to guess passwords. Per-account throttling is layered on top of this
 * inside the auth service, where the email is actually known.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final String INQUIRY_PATH_SUFFIX = "/availability-inquiries";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = bucketFor(request);
        String key = "rl:%s:%s".formatted(bucket.name, clientIp(request));

        RateLimiter.Decision decision = rateLimiter.tryConsume(key, bucket.limit, bucket.window);
        if (decision.permitted()) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS, "Too many requests, please slow down and try again"));
    }

    private Bucket bucketFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith(AUTH_PATH_PREFIX)) {
            return new Bucket("auth", properties.authLimit(), properties.authWindow());
        }
        if (path.endsWith(INQUIRY_PATH_SUFFIX) && "POST".equalsIgnoreCase(request.getMethod())) {
            return new Bucket("inquiry", properties.inquiryLimit(), properties.inquiryWindow());
        }
        return new Bucket("default", properties.defaultLimit(), properties.defaultWindow());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            // The left-most entry is the original client; the rest are proxies.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Bucket(String name, int limit, Duration window) {}
}
