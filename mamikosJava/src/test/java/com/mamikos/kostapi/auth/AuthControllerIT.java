package com.mamikos.kostapi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.auth.web.dto.LoginRequest;
import com.mamikos.kostapi.auth.web.dto.RefreshRequest;
import com.mamikos.kostapi.auth.web.dto.RegisterRequest;
import com.mamikos.kostapi.auth.web.dto.TokenResponse;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import com.mamikos.kostapi.support.TestApi;
import com.mamikos.kostapi.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    void regularUserStartsWithTwentyCredit() {
        AuthResponse response = TestApi.register(restTemplate, "Regular User", UserRole.REGULAR);

        assertThat(response.credit()).isNotNull();
        assertThat(response.credit().balance()).isEqualTo(20);
        assertThat(response.credit().quota()).isEqualTo(20);
    }

    @Test
    void premiumUserStartsWithFortyCredit() {
        AuthResponse response = TestApi.register(restTemplate, "Premium User", UserRole.PREMIUM);

        assertThat(response.credit().balance()).isEqualTo(40);
        assertThat(response.credit().quota()).isEqualTo(40);
    }

    @Test
    void ownerHasNoCreditBlockAtAll() {
        AuthResponse response = TestApi.register(restTemplate, "Owner User", UserRole.OWNER);

        // Not a zero balance: the field itself must be absent, matching "owner will have no
        // credit" being modelled as no wallet rather than an empty one.
        assertThat(response.credit()).isNull();
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() {
        String email = TestApi.uniqueEmail("dup");
        RegisterRequest request =
                new RegisterRequest("First", email, "Password123", "Password123", null, UserRole.REGULAR);

        ResponseEntity<ApiResponse<AuthResponse>> first = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {});
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        RegisterRequest duplicate =
                new RegisterRequest("Second", email, "Password123", "Password123", null, UserRole.REGULAR);
        ResponseEntity<ApiErrorResponse> second = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(duplicate), ApiErrorResponse.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().code()).isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithoutLeakingWhetherTheEmailExists() {
        AuthResponse registered = TestApi.register(restTemplate, "Login User", UserRole.REGULAR);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(registered.user().email(), "WrongPassword1"),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void loginWithAnEmailThatWasNeverRegisteredGivesTheSameGenericError() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(TestApi.uniqueEmail("never-registered"), "Whatever123"),
                ApiErrorResponse.class);

        // Same code and message as a wrong password: confirming "no such account" instead
        // would hand an attacker a free account-enumeration oracle.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void meEndpointReflectsTheAuthenticatedUsersOwnCreditState() {
        AuthResponse owner = TestApi.register(restTemplate, "Me Owner", UserRole.OWNER);

        ResponseEntity<ApiResponse<com.mamikos.kostapi.auth.web.dto.MeResponse>> response = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(owner.token().accessToken())),
                new ParameterizedTypeReference<ApiResponse<com.mamikos.kostapi.auth.web.dto.MeResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().credit()).isNull();
        assertThat(response.getBody().data().user().role()).isEqualTo("OWNER");
    }

    @Test
    void refreshTokenRotatesOnEveryUse() {
        AuthResponse registered = TestApi.register(restTemplate, "Refresh User", UserRole.REGULAR);
        String firstRefreshToken = registered.token().refreshToken();

        ResponseEntity<ApiResponse<TokenResponse>> rotated = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(new RefreshRequest(firstRefreshToken)),
                new ParameterizedTypeReference<ApiResponse<TokenResponse>>() {});

        assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rotated.getBody().data().refreshToken()).isNotEqualTo(firstRefreshToken);
    }

    /**
     * Regression test: replaying an already-rotated refresh token must revoke every other
     * session too, and that revocation must survive the exception thrown right after it —
     * a bug this project actually shipped once, because the revoke and the throw shared one
     * transaction and the default rollback rule silently undid the revoke.
     */
    @Test
    void replayingARotatedRefreshTokenRevokesEverySessionForThatUser() {
        AuthResponse registered = TestApi.register(restTemplate, "Replay User", UserRole.REGULAR);
        String firstRefreshToken = registered.token().refreshToken();

        ResponseEntity<ApiResponse<TokenResponse>> firstRotation = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(new RefreshRequest(firstRefreshToken)),
                new ParameterizedTypeReference<ApiResponse<TokenResponse>>() {});
        String secondRefreshToken = firstRotation.getBody().data().refreshToken();

        ResponseEntity<ApiErrorResponse> replay = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(new RefreshRequest(firstRefreshToken)),
                ApiErrorResponse.class);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<ApiErrorResponse> secondTokenNowRevokedToo = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(new RefreshRequest(secondRefreshToken)),
                ApiErrorResponse.class);
        assertThat(secondTokenNowRevokedToo.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Regression test: logout must invalidate the access token immediately via the Redis
     * denylist, not merely revoke the refresh token — a stateless JWT keeps working on its
     * own otherwise, all the way until it naturally expires.
     */
    @Test
    void logoutDenylistsTheAccessTokenImmediately() {
        AuthResponse registered = TestApi.register(restTemplate, "Logout User", UserRole.REGULAR);
        String accessToken = registered.token().accessToken();

        ResponseEntity<ApiResponse<Object>> meBefore = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(accessToken)),
                new ParameterizedTypeReference<ApiResponse<Object>>() {});
        assertThat(meBefore.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResponse<Void>> logout = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(null, TestApi.bearer(accessToken)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiErrorResponse> meAfter = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(accessToken)),
                ApiErrorResponse.class);
        assertThat(meAfter.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(meAfter.getBody().code()).isEqualTo("UNAUTHENTICATED");
    }
}
