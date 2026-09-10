package com.mamikos.kostapi.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the JWT access token.
 *
 * <p>The token only ever carries the user id and role — never anything mutable like a
 * balance — because those claims are trusted for the token's whole lifetime with no
 * database round trip to refresh them. Every token also gets a unique {@code jti} so a
 * single token can be individually denylisted on logout without needing a database of
 * every access token ever issued.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final Clock clock;
    private final Key signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = buildSigningKey(properties.secret());
    }

    private static Key buildSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException notBase64) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 256 bits (32 bytes); got " + keyBytes.length);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String role) {
        Instant now = Instant.now(clock);
        Instant expiry = now.plus(properties.accessTokenTtl());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith((SecretKey) signingKey)
                .compact();
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }

    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    public Optional<Long> extractUserId(String token) {
        return parseClaims(token).map(claims -> Long.valueOf(claims.getSubject()));
    }

    /** Used at logout time to find out which {@code jti} to denylist and for how long. */
    public Optional<AccessTokenClaims> parseAccessTokenClaims(String token) {
        return parseClaims(token)
                .map(claims -> new AccessTokenClaims(
                        Long.valueOf(claims.getSubject()),
                        claims.getId(),
                        claims.getExpiration().toInstant()));
    }

    private Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) signingKey)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public record AccessTokenClaims(Long userId, String jti, Instant expiresAt) {}
}
