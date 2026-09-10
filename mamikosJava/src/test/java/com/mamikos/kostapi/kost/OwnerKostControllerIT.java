package com.mamikos.kostapi.kost;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.kost.entity.RoomType;
import com.mamikos.kostapi.kost.web.dto.request.AddressRequest;
import com.mamikos.kostapi.kost.web.dto.request.PatchKostRequest;
import com.mamikos.kostapi.kost.web.dto.request.UpdateKostRequest;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import com.mamikos.kostapi.support.TestApi;
import com.mamikos.kostapi.user.entity.UserRole;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OwnerKostControllerIT extends AbstractIntegrationTest {

    @Test
    void ownerCanCreateMoreThanOneKost() {
        String token = TestApi.register(restTemplate, "Multi Owner", UserRole.OWNER)
                .token()
                .accessToken();

        TestApi.createKost(restTemplate, token, "First Kost", new BigDecimal("500000"), "MultiCity");
        TestApi.createKost(restTemplate, token, "Second Kost", new BigDecimal("600000"), "MultiCity");

        ResponseEntity<ApiResponse<List<OwnerKostResponse>>> list = restTemplate.exchange(
                "/api/v1/owner/kosts",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(token)),
                new ParameterizedTypeReference<ApiResponse<List<OwnerKostResponse>>>() {});

        assertThat(list.getBody().data()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void ownerListOnlyShowsTheirOwnKosts() {
        String ownerAToken = TestApi.register(restTemplate, "Owner A", UserRole.OWNER)
                .token()
                .accessToken();
        String ownerBToken = TestApi.register(restTemplate, "Owner B", UserRole.OWNER)
                .token()
                .accessToken();
        TestApi.createKost(restTemplate, ownerAToken, "Owner A Kost", new BigDecimal("500000"), "IsolationCity");

        ResponseEntity<ApiResponse<List<OwnerKostResponse>>> ownerBList = restTemplate.exchange(
                "/api/v1/owner/kosts",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(ownerBToken)),
                new ParameterizedTypeReference<ApiResponse<List<OwnerKostResponse>>>() {});

        assertThat(ownerBList.getBody().data())
                .extracting(OwnerKostResponse::name)
                .doesNotContain("Owner A Kost");
    }

    @Test
    void ownerCannotUpdateAnotherOwnersKost() {
        String ownerAToken = TestApi.register(restTemplate, "Cross Owner A", UserRole.OWNER)
                .token()
                .accessToken();
        String ownerBToken = TestApi.register(restTemplate, "Cross Owner B", UserRole.OWNER)
                .token()
                .accessToken();
        var kost = TestApi.createKost(restTemplate, ownerAToken, "Guarded Kost", new BigDecimal("500000"), "GuardCity");

        PatchKostRequest patch =
                new PatchKostRequest("Hijacked Name", null, null, null, null, null, null, null, null, null, null, null);
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/owner/kosts/{id}",
                HttpMethod.PATCH,
                new HttpEntity<>(patch, TestApi.bearer(ownerBToken)),
                ApiErrorResponse.class,
                kost.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("NOT_KOST_OWNER");
    }

    @Test
    void ownerCannotDeleteAnotherOwnersKost() {
        String ownerAToken = TestApi.register(restTemplate, "Delete Owner A", UserRole.OWNER)
                .token()
                .accessToken();
        String ownerBToken = TestApi.register(restTemplate, "Delete Owner B", UserRole.OWNER)
                .token()
                .accessToken();
        var kost = TestApi.createKost(
                restTemplate, ownerAToken, "Un-deletable Kost", new BigDecimal("500000"), "DeleteGuardCity");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/owner/kosts/{id}",
                HttpMethod.DELETE,
                new HttpEntity<>(null, TestApi.bearer(ownerBToken)),
                ApiErrorResponse.class,
                kost.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("NOT_KOST_OWNER");
    }

    /**
     * Regression test: a wrong-role caller must be rejected before Spring MVC ever
     * validates the request body — @PreAuthorize alone only runs once the handler method
     * is actually invoked, which is after body validation, so an invalid body from the
     * wrong role used to leak a 422 instead of a 403.
     */
    @Test
    void regularUserIsRejectedBeforeBodyValidationEvenWithAnInvalidBody() {
        String regularToken = TestApi.register(restTemplate, "Not An Owner", UserRole.REGULAR)
                .token()
                .accessToken();

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/owner/kosts",
                HttpMethod.POST,
                new HttpEntity<>("{}", TestApi.bearer(regularToken)),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void patchOnlyChangesSuppliedFields() {
        AuthResponse owner = TestApi.register(restTemplate, "Patch Owner", UserRole.OWNER);
        String token = owner.token().accessToken();
        var kost = TestApi.createKost(restTemplate, token, "Original Name", new BigDecimal("700000"), "PatchCity");

        PatchKostRequest patch = new PatchKostRequest(
                null, null, null, null, null, new BigDecimal("999000"), null, null, null, null, null, null);
        ResponseEntity<ApiResponse<OwnerKostResponse>> response = restTemplate.exchange(
                "/api/v1/owner/kosts/{id}",
                HttpMethod.PATCH,
                new HttpEntity<>(patch, TestApi.bearer(token)),
                new ParameterizedTypeReference<ApiResponse<OwnerKostResponse>>() {},
                kost.id());

        OwnerKostResponse updated = response.getBody().data();
        assertThat(updated.name()).isEqualTo("Original Name");
        assertThat(updated.pricePerMonth()).isEqualByComparingTo("999000");
    }

    @Test
    void ownerCanReplaceEveryFieldViaPut() {
        String token = TestApi.register(restTemplate, "Put Owner", UserRole.OWNER)
                .token()
                .accessToken();
        var kost = TestApi.createKost(restTemplate, token, "Before Put", new BigDecimal("500000"), "PutCity");

        UpdateKostRequest replacement = new UpdateKostRequest(
                "After Put",
                "Replaced description",
                new AddressRequest("Jl. Replaced", "New District", "New City", "New Province", "12345"),
                null,
                null,
                new BigDecimal("1200000"),
                RoomType.PUTRI,
                8,
                2,
                false,
                java.util.Set.of("ac", "wifi"),
                java.util.List.of());

        ResponseEntity<ApiResponse<OwnerKostResponse>> response = restTemplate.exchange(
                "/api/v1/owner/kosts/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(replacement, TestApi.bearer(token)),
                new ParameterizedTypeReference<ApiResponse<OwnerKostResponse>>() {},
                kost.id());

        OwnerKostResponse updated = response.getBody().data();
        assertThat(updated.name()).isEqualTo("After Put");
        assertThat(updated.pricePerMonth()).isEqualByComparingTo("1200000");
        assertThat(updated.totalRooms()).isEqualTo(8);
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.facilities()).containsExactlyInAnyOrder("ac", "wifi");
    }

    @Test
    void ownerCanFetchASingleOwnedKostByIdIncludingAvailableRooms() {
        String token = TestApi.register(restTemplate, "Get Owner", UserRole.OWNER)
                .token()
                .accessToken();
        var kost = TestApi.createKost(restTemplate, token, "Gettable Kost", new BigDecimal("500000"), "GetCity");

        ResponseEntity<ApiResponse<OwnerKostResponse>> response = restTemplate.exchange(
                "/api/v1/owner/kosts/{id}",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(token)),
                new ParameterizedTypeReference<ApiResponse<OwnerKostResponse>>() {},
                kost.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().availableRooms()).isEqualTo(4);
    }
}
