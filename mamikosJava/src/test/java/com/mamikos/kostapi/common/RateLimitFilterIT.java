package com.mamikos.kostapi.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.LoginRequest;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Every other IT disables rate limiting for determinism (see {@code application-test.yml}),
 * so this is the one place it is deliberately turned back on — with a tiny limit and a short
 * window — to prove the throttle itself actually works (SEC-08).
 */
@TestPropertySource(properties = {"rate-limit.enabled=true", "rate-limit.auth-limit=2", "rate-limit.auth-window=PT30S"})
class RateLimitFilterIT extends AbstractIntegrationTest {

    @Test
    void exceedingTheAuthLimitReturnsTooManyRequests() {
        LoginRequest request = new LoginRequest("rate-limit-probe@example.test", "irrelevant");

        // The auth bucket allows 2 requests per window here; the 3rd must be throttled
        // regardless of the credentials being wrong, since throttling happens before the
        // request is even authenticated.
        var statuses = IntStream.range(0, 3)
                .mapToObj(i -> restTemplate.postForEntity("/api/v1/auth/login", request, String.class))
                .map(ResponseEntity::getStatusCode)
                .toList();

        assertThat(statuses.get(2)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
