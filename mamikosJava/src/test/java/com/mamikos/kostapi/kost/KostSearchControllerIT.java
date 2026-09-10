package com.mamikos.kostapi.kost;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.kost.web.dto.response.KostDetailResponse;
import com.mamikos.kostapi.kost.web.dto.response.KostSummaryResponse;
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

class KostSearchControllerIT extends AbstractIntegrationTest {

    private String ownerToken() {
        return TestApi.register(restTemplate, "Search Owner", UserRole.OWNER)
                .token()
                .accessToken();
    }

    @Test
    void searchIsSortedByPriceAscending() {
        String owner = ownerToken();
        String city = "SortTestCity" + System.nanoTime();
        TestApi.createKost(restTemplate, owner, "Cheap Kost", new BigDecimal("500000"), city);
        TestApi.createKost(restTemplate, owner, "Expensive Kost", new BigDecimal("2000000"), city);
        TestApi.createKost(restTemplate, owner, "Mid Kost", new BigDecimal("1000000"), city);

        ResponseEntity<ApiResponse<List<KostSummaryResponse>>> response = restTemplate.exchange(
                "/api/v1/kosts?location={city}&sortBy=price&order=asc",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<KostSummaryResponse>>>() {},
                city);

        List<KostSummaryResponse> results = response.getBody().data();
        assertThat(results)
                .extracting(KostSummaryResponse::pricePerMonth)
                .containsExactly(
                        new BigDecimal("500000.00"), new BigDecimal("1000000.00"), new BigDecimal("2000000.00"));
    }

    @Test
    void searchWithUnknownSortByIsRejected() {
        ResponseEntity<ApiErrorResponse> response =
                restTemplate.exchange("/api/v1/kosts?sortBy=ownerId", HttpMethod.GET, null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void searchResultsNeverExposeAvailableRoomsField() {
        String owner = ownerToken();
        String city = "PrivacyCity" + System.nanoTime();
        TestApi.createKost(restTemplate, owner, "Private Rooms Kost", new BigDecimal("750000"), city);

        ResponseEntity<String> raw = restTemplate.getForEntity("/api/v1/kosts?location={city}", String.class, city);

        assertThat(raw.getBody()).doesNotContain("availableRooms");
        assertThat(raw.getBody()).contains("\"disclosed\":false");
    }

    @Test
    void publicDetailHidesAvailabilityButOwnerViewShowsIt() {
        AuthResponse owner = TestApi.register(restTemplate, "Detail Owner", UserRole.OWNER);
        var kost = TestApi.createKost(
                restTemplate, owner.token().accessToken(), "Detail Kost", new BigDecimal("900000"), "DetailCity");

        ResponseEntity<ApiResponse<KostDetailResponse>> publicDetail = restTemplate.exchange(
                "/api/v1/kosts/{id}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<KostDetailResponse>>() {},
                kost.id());
        assertThat(publicDetail.getBody().data().availability().disclosed()).isFalse();

        ResponseEntity<ApiErrorResponse> deletedLookup =
                restTemplate.exchange("/api/v1/kosts/999999999", HttpMethod.GET, null, ApiErrorResponse.class);
        assertThat(deletedLookup.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void softDeletedKostDisappearsFromSearchAndDetail() {
        AuthResponse owner = TestApi.register(restTemplate, "Delete Owner", UserRole.OWNER);
        String token = owner.token().accessToken();
        var kost = TestApi.createKost(restTemplate, token, "Doomed Kost", new BigDecimal("600000"), "DoomedCity");

        restTemplate.exchange(
                "/api/v1/owner/kosts/{id}",
                HttpMethod.DELETE,
                new HttpEntity<>(null, TestApi.bearer(token)),
                Void.class,
                kost.id());

        ResponseEntity<ApiErrorResponse> detail =
                restTemplate.exchange("/api/v1/kosts/{id}", HttpMethod.GET, null, ApiErrorResponse.class, kost.id());
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
