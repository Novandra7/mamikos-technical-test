package com.mamikos.kostapi.common.ratelimit;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter kept in Redis so the limit holds across every API instance.
 *
 * <p>The increment and the expiry have to be one atomic step; doing them as two round
 * trips leaves a window where a crash between them creates a key that never expires and
 * locks the caller out permanently. A Lua script gives that atomicity in a single call.
 */
@Component
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final RedisScript<List> INCREMENT_AND_EXPIRE = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return { current, redis.call('PTTL', KEYS[1]) }
            """,
            List.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Decision tryConsume(String key, int limit, Duration window) {
        try {
            List<?> result = redis.execute(INCREMENT_AND_EXPIRE, List.of(key), String.valueOf(window.toMillis()));
            if (result == null || result.size() < 2) {
                return Decision.allowed();
            }
            long hits = ((Number) result.get(0)).longValue();
            long ttlMillis = ((Number) result.get(1)).longValue();
            if (hits > limit) {
                long retryAfter = ttlMillis > 0 ? (ttlMillis + 999) / 1000 : window.toSeconds();
                return Decision.denied(retryAfter);
            }
            return Decision.allowed();
        } catch (RuntimeException ex) {
            // Fail open. A rate limiter that is itself unavailable must not take the
            // whole API down with it; losing throttling is the lesser failure.
            log.warn("Rate limiter unavailable, allowing request for key {}: {}", key, ex.getMessage());
            return Decision.allowed();
        }
    }
}
