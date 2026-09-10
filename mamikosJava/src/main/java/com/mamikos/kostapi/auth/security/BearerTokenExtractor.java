package com.mamikos.kostapi.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.util.StringUtils;

/** Shared by the authentication filter and the logout endpoint so both parse the header
 * the same way. */
public final class BearerTokenExtractor {

    private static final String PREFIX = "Bearer ";

    private BearerTokenExtractor() {}

    public static Optional<String> extract(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return Optional.of(header.substring(PREFIX.length()));
        }
        return Optional.empty();
    }
}
