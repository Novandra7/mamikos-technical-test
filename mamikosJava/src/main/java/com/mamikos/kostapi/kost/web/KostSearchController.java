package com.mamikos.kostapi.kost.web;

import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.common.response.PaginationMeta;
import com.mamikos.kostapi.kost.entity.RoomType;
import com.mamikos.kostapi.kost.service.KostSearchService;
import com.mamikos.kostapi.kost.service.KostService;
import com.mamikos.kostapi.kost.web.dto.request.KostSearchRequest;
import com.mamikos.kostapi.kost.web.dto.response.KostDetailResponse;
import com.mamikos.kostapi.kost.web.dto.response.KostSummaryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated endpoints: anyone can search kosts and view a kost's detail. */
@RestController
@RequestMapping("/api/v1/kosts")
@Tag(name = "Kosts", description = "Public kost search and detail — no room-availability numbers here")
public class KostSearchController {

    private final KostSearchService kostSearchService;
    private final KostService kostService;

    public KostSearchController(KostSearchService kostSearchService, KostService kostService) {
        this.kostSearchService = kostSearchService;
        this.kostService = kostService;
    }

    @GetMapping
    public ApiResponse<List<KostSummaryResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        KostSearchRequest request =
                new KostSearchRequest(name, location, priceMin, priceMax, roomType, sortBy, order, page, perPage);
        Page<KostSummaryResponse> result = kostSearchService.search(request);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", PaginationMeta.of(result));
        meta.put(
                "filtersApplied",
                filtersApplied(request.name(), request.location(), request.priceMin(), request.priceMax()));
        meta.put("sort", Map.of("by", request.sortBy(), "order", request.order()));

        return ApiResponse.of("Kost list retrieved", result.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<KostDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.of("Kost detail retrieved", kostService.getPublicDetail(id));
    }

    private Map<String, Object> filtersApplied(String name, String location, BigDecimal priceMin, BigDecimal priceMax) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (name != null && !name.isBlank()) {
            filters.put("name", name);
        }
        if (location != null && !location.isBlank()) {
            filters.put("location", location);
        }
        if (priceMin != null) {
            filters.put("priceMin", priceMin);
        }
        if (priceMax != null) {
            filters.put("priceMax", priceMax);
        }
        return filters;
    }
}
