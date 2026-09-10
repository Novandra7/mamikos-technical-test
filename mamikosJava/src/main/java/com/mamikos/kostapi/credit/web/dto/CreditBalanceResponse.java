package com.mamikos.kostapi.credit.web.dto;

import java.time.Instant;

public record CreditBalanceResponse(
        String role,
        int balance,
        int quota,
        int inquiryCost,
        int remainingInquiries,
        Instant lastRechargedAt,
        Instant nextRechargeAt) {}
