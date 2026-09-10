package com.mamikos.kostapi.credit.web.dto;

import java.time.Instant;

public record CreditTransactionResponse(
        Long id, String type, int amount, int balanceBefore, int balanceAfter, String description, Instant createdAt) {}
