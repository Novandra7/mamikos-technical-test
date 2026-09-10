package com.mamikos.kostapi.kost.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Owner-facing view of their own kost: includes availableRooms and the inquiry count. */
public record OwnerKostResponse(
        Long id,
        String name,
        String description,
        AddressResponse address,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal pricePerMonth,
        String roomType,
        int totalRooms,
        int availableRooms,
        boolean isActive,
        List<String> facilities,
        List<String> photos,
        long inquiriesCount,
        Instant createdAt,
        Instant updatedAt) {}
