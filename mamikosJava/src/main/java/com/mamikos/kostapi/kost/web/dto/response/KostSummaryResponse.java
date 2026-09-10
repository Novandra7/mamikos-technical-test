package com.mamikos.kostapi.kost.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Public search-result row. Deliberately has no {@code availableRooms} field at all. */
public record KostSummaryResponse(
        Long id,
        String name,
        AddressResponse address,
        BigDecimal pricePerMonth,
        String roomType,
        int totalRooms,
        List<String> facilities,
        List<String> photos,
        OwnerSummaryResponse owner,
        AvailabilityResponse availability,
        Instant createdAt) {}
