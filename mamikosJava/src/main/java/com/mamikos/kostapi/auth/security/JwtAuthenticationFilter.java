package com.mamikos.kostapi.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token, verifies it, and — if valid and not denylisted — puts an
 * authenticated principal in the security context for the rest of the request. No session
 * is ever created; every request re-proves itself from its own token, which is what
 * "stateless" API auth means.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final UserRepositoryUserDetailsLookup userLookup;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TokenDenylistService tokenDenylistService,
            UserRepositoryUserDetailsLookup userLookup) {
        this.jwtService = jwtService;
        this.tokenDenylistService = tokenDenylistService;
        this.userLookup = userLookup;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        BearerTokenExtractor.extract(request)
                .flatMap(jwtService::parseAccessTokenClaims)
                // A signature- and expiry-valid token that was explicitly logged out must
                // still be rejected — that is the entire point of the denylist, so it is
                // checked before the token is ever trusted to authenticate the request.
                .filter(claims -> !tokenDenylistService.isDenylisted(claims.jti()))
                .flatMap(claims -> userLookup.findById(claims.userId()))
                .ifPresent(securityUser -> {
                    var authentication =
                            new PreAuthenticatedAuthenticationToken(securityUser, null, securityUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Marking it authenticated here is safe only because the token
                    // signature and expiry were already verified inside JwtService.
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        chain.doFilter(request, response);
    }

    /** Narrow seam so this filter depends on "look up a user by id", not the whole repository. */
    public interface UserRepositoryUserDetailsLookup {
        Optional<SecurityUser> findById(Long id);
    }
}
