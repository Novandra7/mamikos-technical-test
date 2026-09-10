package com.mamikos.kostapi.auth.web.dto;

public record AuthResponse(UserResponse user, CreditSummaryResponse credit, TokenResponse token) {}
