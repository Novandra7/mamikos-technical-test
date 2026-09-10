package com.mamikos.kostapi.inquiry.web.dto;

import java.time.Instant;

public record InquiryResponse(
        Long id, KostRefResponse kost, String message, String status, String ownerReply, Instant createdAt) {}
