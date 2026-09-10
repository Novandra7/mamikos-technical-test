package com.mamikos.kostapi.credit.web;

import com.mamikos.kostapi.auth.security.SecurityUser;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.common.response.PaginationMeta;
import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.service.CreditService;
import com.mamikos.kostapi.credit.web.dto.CreditBalanceResponse;
import com.mamikos.kostapi.credit.web.dto.CreditTransactionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owners have no wallet at all, so every endpoint here is closed to them (US-09). */
@RestController
@RequestMapping("/api/v1/me/credits")
@PreAuthorize("hasAnyRole('REGULAR', 'PREMIUM')")
@Tag(name = "Credits", description = "The caller's own credit balance and ledger")
public class CreditController {

    private final CreditService creditService;
    private final CreditProperties creditProperties;

    public CreditController(CreditService creditService, CreditProperties creditProperties) {
        this.creditService = creditService;
        this.creditProperties = creditProperties;
    }

    @GetMapping
    public ApiResponse<CreditBalanceResponse> balance(@AuthenticationPrincipal SecurityUser principal) {
        CreditBalance balance = creditService.requireBalance(principal.id());
        int quota = creditProperties.quotaFor(principal.role());
        int cost = creditProperties.inquiryCost();

        return ApiResponse.of(
                "Credit balance retrieved",
                new CreditBalanceResponse(
                        principal.role().name(),
                        balance.getBalance(),
                        quota,
                        cost,
                        balance.getBalance() / cost,
                        balance.getLastRechargedAt(),
                        nextRechargeAt()));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<CreditTransactionResponse>> transactions(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        int safePerPage = Math.min(Math.max(perPage, 1), 50);
        var pageable = PageRequest.of(Math.max(page - 1, 0), safePerPage);
        Page<CreditTransactionResponse> result = creditService
                .history(principal.id(), pageable)
                .map(tx -> new CreditTransactionResponse(
                        tx.getId(),
                        tx.getType().name(),
                        tx.getAmount(),
                        tx.getBalanceBefore(),
                        tx.getBalanceAfter(),
                        tx.getDescription(),
                        tx.getCreatedAt()));

        return ApiResponse.of(
                "Credit transaction history retrieved",
                result.getContent(),
                Map.of("pagination", PaginationMeta.of(result)));
    }

    private Instant nextRechargeAt() {
        ZoneId zone = ZoneId.of(creditProperties.timezone());
        YearMonth nextMonth = YearMonth.now(zone).plusMonths(1);
        return nextMonth.atDay(1).atStartOfDay(zone).toInstant();
    }
}
