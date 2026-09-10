package com.mamikos.kostapi.inquiry;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.inquiry.web.dto.CreateInquiryRequest;
import com.mamikos.kostapi.inquiry.web.dto.InquiryCreatedResponse;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import com.mamikos.kostapi.support.TestApi;
import com.mamikos.kostapi.user.entity.UserRole;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AvailabilityInquiryControllerIT extends AbstractIntegrationTest {

    @Test
    void firstInquiryChargesFiveCreditsAndRevealsAvailability() {
        AuthResponse owner = TestApi.register(restTemplate, "Inquiry Owner", UserRole.OWNER);
        OwnerKostResponse kost = TestApi.createKost(
                restTemplate, owner.token().accessToken(), "Inquiry Kost", new BigDecimal("500000"), "InquiryCity");
        AuthResponse user = TestApi.register(restTemplate, "Inquiry User", UserRole.REGULAR);

        ResponseEntity<ApiResponse<InquiryCreatedResponse>> response = restTemplate.exchange(
                "/api/v1/kosts/{id}/availability-inquiries",
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateInquiryRequest("Ada kamar kosong?"),
                        TestApi.bearer(user.token().accessToken())),
                new ParameterizedTypeReference<ApiResponse<InquiryCreatedResponse>>() {},
                kost.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        InquiryCreatedResponse body = response.getBody().data();
        assertThat(body.credit().charged()).isEqualTo(5);
        assertThat(body.credit().balanceBefore()).isEqualTo(20);
        assertThat(body.credit().balanceAfter()).isEqualTo(15);
        assertThat(body.availability().disclosed()).isTrue();
        assertThat(body.availability().availableRooms()).isEqualTo(4);
    }

    @Test
    void ownerCannotAskAboutAvailability() {
        AuthResponse owner = TestApi.register(restTemplate, "Self Owner", UserRole.OWNER);
        OwnerKostResponse kost = TestApi.createKost(
                restTemplate, owner.token().accessToken(), "Self Kost", new BigDecimal("500000"), "SelfCity");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/kosts/{id}/availability-inquiries",
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateInquiryRequest("test"),
                        TestApi.bearer(owner.token().accessToken())),
                ApiErrorResponse.class,
                kost.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void insufficientCreditLeavesBalanceAndInquiryCountUntouched() {
        AuthResponse owner = TestApi.register(restTemplate, "Poor Owner", UserRole.OWNER);
        OwnerKostResponse kost = TestApi.createKost(
                restTemplate, owner.token().accessToken(), "Poor Kost", new BigDecimal("500000"), "PoorCity");
        AuthResponse user = TestApi.register(restTemplate, "Poor User", UserRole.REGULAR);
        String userToken = user.token().accessToken();

        // Spend all 20 credits (4 inquiries at 5 each).
        for (int i = 0; i < 4; i++) {
            restTemplate.exchange(
                    "/api/v1/kosts/{id}/availability-inquiries",
                    HttpMethod.POST,
                    new HttpEntity<>(new CreateInquiryRequest("again"), TestApi.bearer(userToken)),
                    Object.class,
                    kost.id());
        }

        ResponseEntity<ApiErrorResponse> fifth = restTemplate.exchange(
                "/api/v1/kosts/{id}/availability-inquiries",
                HttpMethod.POST,
                new HttpEntity<>(new CreateInquiryRequest("one more"), TestApi.bearer(userToken)),
                ApiErrorResponse.class,
                kost.id());

        assertThat(fifth.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(fifth.getBody().code()).isEqualTo("INSUFFICIENT_CREDIT");

        ResponseEntity<ApiResponse<Object>> balance = restTemplate.exchange(
                "/api/v1/me/credits",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(userToken)),
                new ParameterizedTypeReference<ApiResponse<Object>>() {});
        assertThat(balance.getBody().data().toString()).contains("balance=0");
    }

    @Test
    void inquiryAgainstMissingKostReturns404WithoutTouchingCredit() {
        AuthResponse user = TestApi.register(restTemplate, "NoKost User", UserRole.REGULAR);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/kosts/{id}/availability-inquiries",
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateInquiryRequest("test"),
                        TestApi.bearer(user.token().accessToken())),
                ApiErrorResponse.class,
                999999999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void ownerCanReplyToAnInquiryOnce() {
        AuthResponse owner = TestApi.register(restTemplate, "Reply Owner", UserRole.OWNER);
        String ownerToken = owner.token().accessToken();
        OwnerKostResponse kost =
                TestApi.createKost(restTemplate, ownerToken, "Reply Kost", new BigDecimal("500000"), "ReplyCity");
        AuthResponse user = TestApi.register(restTemplate, "Reply User", UserRole.REGULAR);

        ResponseEntity<ApiResponse<InquiryCreatedResponse>> created = restTemplate.exchange(
                "/api/v1/kosts/{id}/availability-inquiries",
                HttpMethod.POST,
                new HttpEntity<>(
                        new CreateInquiryRequest("test"),
                        TestApi.bearer(user.token().accessToken())),
                new ParameterizedTypeReference<ApiResponse<InquiryCreatedResponse>>() {},
                kost.id());
        Long inquiryId = created.getBody().data().inquiry().id();

        ResponseEntity<String> firstReply = restTemplate.exchange(
                "/api/v1/owner/inquiries/{id}/reply",
                HttpMethod.POST,
                new HttpEntity<>(java.util.Map.of("reply", "Masih ada"), TestApi.bearer(ownerToken)),
                String.class,
                inquiryId);
        assertThat(firstReply.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiErrorResponse> secondReply = restTemplate.exchange(
                "/api/v1/owner/inquiries/{id}/reply",
                HttpMethod.POST,
                new HttpEntity<>(java.util.Map.of("reply", "Lagi"), TestApi.bearer(ownerToken)),
                ApiErrorResponse.class,
                inquiryId);
        assertThat(secondReply.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(secondReply.getBody().code()).isEqualTo("INQUIRY_ALREADY_ANSWERED");
    }
}
