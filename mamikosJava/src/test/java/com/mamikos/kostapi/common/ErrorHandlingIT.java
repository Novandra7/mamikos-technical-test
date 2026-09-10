package com.mamikos.kostapi.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.common.response.ApiErrorResponse;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Covers the {@code GlobalExceptionHandler} branches the feature-specific IT suites do not
 * happen to exercise on their way to testing business rules. */
class ErrorHandlingIT extends AbstractIntegrationTest {

    @Test
    void malformedJsonBodyReturnsBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>("{not valid json", headers),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void unsupportedHttpMethodReturns405() {
        ResponseEntity<ApiErrorResponse> response =
                restTemplate.exchange("/api/v1/auth/register", HttpMethod.DELETE, null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void unauthenticatedRequestToAnUnknownRouteIsRejectedBeforeMvcEvenLooksForAHandler() {
        // Security is default-deny at the filter chain: an unmapped path behind a request
        // with no token is refused as unauthenticated, not exposed as a distinguishable 404 —
        // the alternative would let an unauthenticated caller fingerprint which routes exist.
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/this-route-does-not-exist", HttpMethod.GET, null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedRequestToAnUnknownRouteReturns404() {
        String token = com.mamikos.kostapi.support.TestApi.register(
                        restTemplate, "404 Prober", com.mamikos.kostapi.user.entity.UserRole.REGULAR)
                .token()
                .accessToken();

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/this-route-does-not-exist",
                HttpMethod.GET,
                new HttpEntity<>(null, com.mamikos.kostapi.support.TestApi.bearer(token)),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void nonNumericPathVariableReturnsBadRequest() {
        ResponseEntity<ApiErrorResponse> response =
                restTemplate.exchange("/api/v1/kosts/not-a-number", HttpMethod.GET, null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void beanValidationFailureReturns422WithFieldErrors() {
        String body =
                """
                {"name":"A","email":"not-an-email","password":"short","passwordConfirmation":"short","role":"regular"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(body, headers), ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().errors()).containsKeys("email", "password");
    }

    @Test
    void mismatchedPasswordConfirmationFailsClassLevelValidation() {
        String body =
                """
                {"name":"Valid Name","email":"mismatch-test@example.test","password":"Password123",\
                "passwordConfirmation":"Different123","role":"regular"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(body, headers), ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().errors()).containsKey("passwordConfirmation");
    }
}
