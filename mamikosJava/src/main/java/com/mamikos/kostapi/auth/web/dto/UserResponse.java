package com.mamikos.kostapi.auth.web.dto;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, String phone, String role, Instant createdAt) {}
