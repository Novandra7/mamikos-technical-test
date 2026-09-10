package com.mamikos.kostapi.auth.service;

import com.mamikos.kostapi.auth.entity.RefreshToken;
import com.mamikos.kostapi.auth.repository.RefreshTokenRepository;
import com.mamikos.kostapi.auth.security.JwtService;
import com.mamikos.kostapi.auth.security.TokenHasher;
import com.mamikos.kostapi.common.exception.DomainException;
import com.mamikos.kostapi.common.exception.ErrorCode;
import com.mamikos.kostapi.user.entity.User;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates, and revokes refresh tokens.
 *
 * <p>Rotation on every use means a stolen-but-unused token has a single chance to be
 * replayed before it stops working; replaying an already-rotated token is treated as a
 * signal of compromise and revokes every other session the user has open.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = randomToken();
        Instant expiresAt = Instant.now(clock).plus(jwtService.refreshTokenTtl());
        refreshTokenRepository.save(RefreshToken.issue(user, TokenHasher.sha256(rawToken), expiresAt));
        return rawToken;
    }

    /**
     * Verifies a presented refresh token and rotates it.
     *
     * <p>{@code noRollbackFor} matters here: on replay, this method deliberately revokes
     * every session for the user and then throws — and without it, Spring's default
     * rollback-on-unchecked-exception behaviour would undo that revocation along with the
     * exception, silently defeating the whole point of detecting the replay.
     *
     * @return the user the token belonged to, so the caller can mint a fresh access token
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public User rotate(String rawToken) {
        String hash = TokenHasher.sha256(rawToken);
        RefreshToken token =
                refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            // Someone is replaying a token that was already rotated or logged out — treat
            // it as compromised and kill every session this user currently holds.
            log.warn(
                    "Refresh token replay detected for user {}", token.getUser().getId());
            refreshTokenRepository.revokeAllActiveForUser(token.getUser().getId(), Instant.now(clock));
            throw new InvalidRefreshTokenException();
        }
        if (token.isExpired(Instant.now(clock))) {
            throw new InvalidRefreshTokenException();
        }

        token.revoke(Instant.now(clock));
        return token.getUser();
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now(clock));
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class InvalidRefreshTokenException extends DomainException {
        public InvalidRefreshTokenException() {
            super(ErrorCode.UNAUTHENTICATED, "Refresh token is invalid, expired, or has already been used");
        }
    }
}
