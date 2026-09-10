package com.mamikos.kostapi.inquiry.web.dto;

public record InquiryAvailabilityResponse(
        boolean disclosed, int availableRooms, int totalRooms, boolean isAvailable, java.time.Instant asOf) {}
