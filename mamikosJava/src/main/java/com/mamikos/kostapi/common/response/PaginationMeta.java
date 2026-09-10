package com.mamikos.kostapi.common.response;

import org.springframework.data.domain.Page;

/** Pagination block exposed under {@code meta.pagination}; pages are 1-based on the wire. */
public record PaginationMeta(int currentPage, int perPage, long total, int lastPage, Integer from, Integer to) {

    public static PaginationMeta of(Page<?> page) {
        int currentPage = page.getNumber() + 1;
        int numberOfElements = page.getNumberOfElements();
        Integer from = numberOfElements == 0 ? null : (page.getNumber() * page.getSize()) + 1;
        Integer to = numberOfElements == 0 ? null : from + numberOfElements - 1;
        return new PaginationMeta(
                currentPage, page.getSize(), page.getTotalElements(), Math.max(page.getTotalPages(), 1), from, to);
    }
}
