package com.mamikos.kostapi.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hashes opaque refresh tokens before they are persisted, the same reasoning as for passwords:
 * a database leak must not hand out live sessions. */
public final class TokenHasher {

    private TokenHasher() {}

    public static String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist in every conforming JDK; this can never happen.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
