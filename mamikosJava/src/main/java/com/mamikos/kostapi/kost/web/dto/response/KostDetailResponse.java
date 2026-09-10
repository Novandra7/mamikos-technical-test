package com.mamikos.kostapi.kost.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Public detail view. Same non-disclosure rule as the search result. */
public record KostDetailResponse(
        Long id,
        String name,
        String description,
        AddressResponse address,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal pricePerMonth,
        String roomType,
        int totalRooms,
        List<String> facilities,
        List<String> photos,
        OwnerSummaryResponse owner,
        AvailabilityResponse availability,
        Instant createdAt) {}
