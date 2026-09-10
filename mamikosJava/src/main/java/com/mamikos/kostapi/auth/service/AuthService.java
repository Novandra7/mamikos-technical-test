package com.mamikos.kostapi.auth.service;

import com.mamikos.kostapi.auth.mapper.UserMapper;
import com.mamikos.kostapi.auth.security.JwtService;
import com.mamikos.kostapi.auth.security.TokenDenylistService;
import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.auth.web.dto.CreditSummaryResponse;
import com.mamikos.kostapi.auth.web.dto.LoginRequest;
import com.mamikos.kostapi.auth.web.dto.MeResponse;
import com.mamikos.kostapi.auth.web.dto.RegisterRequest;
import com.mamikos.kostapi.auth.web.dto.TokenResponse;
import com.mamikos.kostapi.common.exception.EmailAlreadyRegisteredException;
import com.mamikos.kostapi.common.exception.UnauthenticatedException;
import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.service.CreditGrantService;
import com.mamikos.kostapi.user.entity.User;
import com.mamikos.kostapi.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates registration, login, refresh, and logout.
 *
 * <p>{@link #register} is the one place BR-10 ("user + wallet + initial transaction in one
 * database transaction") is enforced: the whole method is one {@code @Transactional}
 * boundary, and {@link CreditGrantService} refuses to run outside of it.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditGrantService creditGrantService;
    private final CreditProperties creditProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public AuthService(
            UserRepository userRepository,
            CreditBalanceRepository creditBalanceRepository,
            CreditGrantService creditGrantService,
            CreditProperties creditProperties,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            TokenDenylistService tokenDenylistService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.creditBalanceRepository = creditBalanceRepository;
        this.creditGrantService = creditGrantService;
        this.creditProperties = creditProperties;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenDenylistService = tokenDenylistService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normaliseEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
                .name(request.name())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .phone(request.phone())
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException raceOnUniqueEmail) {
            // Two concurrent registrations for the same address can both pass the
            // existsByEmail check above; the unique index is the actual source of truth.
            throw new EmailAlreadyRegisteredException();
        }

        creditGrantService.grantInitialCreditIfApplicable(user);

        CreditSummaryResponse creditSummary = user.hasCreditWallet()
                ? new CreditSummaryResponse(
                        creditProperties.quotaFor(user.getRole()), creditProperties.quotaFor(user.getRole()))
                : null;

        String accessToken =
                jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);

        return new AuthResponse(
                userMapper.toResponse(user),
                creditSummary,
                new TokenResponse(
                        accessToken,
                        refreshToken,
                        "Bearer",
                        jwtService.accessTokenTtl().toSeconds()));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = User.normaliseEmail(request.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (org.springframework.security.core.AuthenticationException failed) {
            // Deliberately generic: confirming "that email doesn't exist" vs "wrong
            // password" hands an attacker a free account-enumeration oracle.
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository
                .findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String accessToken =
                jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        CreditSummaryResponse creditSummary = loadCreditSummary(user);

        return new AuthResponse(
                userMapper.toResponse(user),
                creditSummary,
                new TokenResponse(
                        accessToken,
                        refreshToken,
                        "Bearer",
                        jwtService.accessTokenTtl().toSeconds()));
    }

    /**
     * {@code noRollbackFor} has to be repeated here, not just on {@link
     * RefreshTokenService#rotate}: with the default {@code REQUIRED} propagation,
     * {@code rotate} merely joins this method's transaction rather than opening its own, so
     * this outermost boundary is the one whose rollback rule actually governs whether the
     * replay-detection revoke it performs is committed or undone along with the exception.
     */
    @Transactional(noRollbackFor = RefreshTokenService.InvalidRefreshTokenException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        User user = refreshTokenService.rotate(rawRefreshToken);
        String accessToken =
                jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = refreshTokenService.issue(user);
        return new TokenResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                jwtService.accessTokenTtl().toSeconds());
    }

    /**
     * Revokes the refresh token family and denylists the specific access token used to call
     * this endpoint, so it stops working immediately rather than lingering until it expires
     * on its own (US-02).
     */
    @Transactional
    public void logout(Long userId, JwtService.AccessTokenClaims currentAccessToken) {
        refreshTokenService.revokeAllForUser(userId);
        if (currentAccessToken != null) {
            tokenDenylistService.denylist(currentAccessToken.jti(), currentAccessToken.expiresAt());
        }
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository
                .findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new UnauthenticatedException("User no longer exists"));
        return new MeResponse(userMapper.toResponse(user), loadCreditSummary(user));
    }

    private CreditSummaryResponse loadCreditSummary(User user) {
        if (!user.hasCreditWallet()) {
            return null;
        }
        int quota = creditProperties.quotaFor(user.getRole());
        return creditBalanceRepository
                .findByUserId(user.getId())
                .map(balance -> new CreditSummaryResponse(balance.getBalance(), quota))
                .orElse(new CreditSummaryResponse(0, quota));
    }
}
