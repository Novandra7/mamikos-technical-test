package com.mamikos.kostapi.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Tracks access tokens that were explicitly logged out, keyed by their {@code jti}.
 *
 * <p>A JWT is normally valid until it expires no matter what the server does — there is no
 * session to delete. Logout has to work anyway, so the one token being logged out gets
 * listed here for exactly as long as it would otherwise have been valid; every other access
 * token from the same user keeps working until it naturally expires, same as any stateless
 * JWT design. The list lives in Redis, not the database, because it is disposable by
 * construction: every entry expires on its own.
 */
@Service
public class TokenDenylistService {

    private static final Logger log = LoggerFactory.getLogger(TokenDenylistService.class);
    private static final String KEY_PREFIX = "auth:denylist:jti:";

    private final StringRedisTemplate redis;
    private final Clock clock;

    public TokenDenylistService(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    public void denylist(String jti, Instant tokenExpiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), tokenExpiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            // Already expired on its own; nothing to add.
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + jti, "revoked", ttl);
        } catch (RuntimeException redisUnavailable) {
            // Fail open, same reasoning as the rate limiter: losing this defense-in-depth
            // check must not take the whole API down. Refresh-token revocation (the other
            // half of logout) still went through even if this call fails.
            log.warn("Token denylist unavailable, jti={} was not recorded: {}", jti, redisUnavailable.getMessage());
        }
    }

    public boolean isDenylisted(String jti) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
        } catch (RuntimeException redisUnavailable) {
            log.warn(
                    "Token denylist unavailable, treating jti={} as not denylisted: {}",
                    jti,
                    redisUnavailable.getMessage());
            return false;
        }
    }
}
