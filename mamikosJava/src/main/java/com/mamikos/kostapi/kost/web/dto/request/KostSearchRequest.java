package com.mamikos.kostapi.kost.web.dto.request;

import com.mamikos.kostapi.kost.entity.RoomType;
import java.math.BigDecimal;

/**
 * Bound from query parameters. {@code sortBy} is validated against a fixed whitelist
 * ({@link com.mamikos.kostapi.kost.service.KostSearchService}) rather than being handed to
 * {@code Pageable} directly — an unchecked sort property would let a client sort by (or
 * probe for) an internal column name.
 */
public record KostSearchRequest(
        String name,
        String location,
        BigDecimal priceMin,
        BigDecimal priceMax,
        RoomType roomType,
        String sortBy,
        String order,
        int page,
        int perPage) {

    public KostSearchRequest {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (order == null || order.isBlank()) {
            order = "asc";
        }
        if (page < 1) {
            page = 1;
        }
        if (perPage < 1) {
            perPage = 10;
        }
        if (perPage > 50) {
            perPage = 50;
        }
    }
}
