package com.mamikos.kostapi.credit;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.credit.web.dto.CreditBalanceResponse;
import com.mamikos.kostapi.credit.web.dto.CreditTransactionResponse;
import com.mamikos.kostapi.inquiry.web.dto.CreateInquiryRequest;
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

class CreditControllerIT extends AbstractIntegrationTest {

    @Test
    void balanceEndpointReportsQuotaAndRemainingInquiries() {
        AuthResponse user = TestApi.register(restTemplate, "Balance User", UserRole.REGULAR);

        ResponseEntity<ApiResponse<CreditBalanceResponse>> response = restTemplate.exchange(
                "/api/v1/me/credits",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(user.token().accessToken())),
                new ParameterizedTypeReference<ApiResponse<CreditBalanceResponse>>() {});

        CreditBalanceResponse body = response.getBody().data();
        assertThat(body.balance()).isEqualTo(20);
        assertThat(body.quota()).isEqualTo(20);
        assertThat(body.inquiryCost()).isEqualTo(5);
        assertThat(body.remainingInquiries()).isEqualTo(4);
    }

    @Test
    void transactionHistoryShowsInitialGrantThenDeduction() {
        AuthResponse owner = TestApi.register(restTemplate, "Ledger Owner", UserRole.OWNER);
        OwnerKostResponse kost = TestApi.createKost(
                restTemplate, owner.token().accessToken(), "Ledger Kost", new BigDecimal("500000"), "LedgerCity");
        AuthResponse user = TestApi.register(restTemplate, "Ledger User", UserRole.REGULAR);
        String token = user.token().accessToken();

        restTemplate.exchange(
                "/api/v1/kosts/{id}/availability-inquiries",
                HttpMethod.POST,
                new HttpEntity<>(new CreateInquiryRequest("test"), TestApi.bearer(token)),
                Object.class,
                kost.id());

        ResponseEntity<ApiResponse<List<CreditTransactionResponse>>> response = restTemplate.exchange(
                "/api/v1/me/credits/transactions",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(token)),
                new ParameterizedTypeReference<ApiResponse<List<CreditTransactionResponse>>>() {});

        List<CreditTransactionResponse> transactions = response.getBody().data();
        assertThat(transactions)
                .extracting(CreditTransactionResponse::type)
                .containsExactly("INQUIRY_DEDUCTION", "INITIAL_GRANT");
    }

    @Test
    void ownersCannotAccessCreditEndpointsAtAll() {
        AuthResponse owner = TestApi.register(restTemplate, "No Credit Owner", UserRole.OWNER);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/me/credits",
                HttpMethod.GET,
                new HttpEntity<>(null, TestApi.bearer(owner.token().accessToken())),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
