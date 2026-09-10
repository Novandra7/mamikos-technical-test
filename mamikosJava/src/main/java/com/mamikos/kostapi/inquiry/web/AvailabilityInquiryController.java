package com.mamikos.kostapi.inquiry.web;

import com.mamikos.kostapi.auth.security.SecurityUser;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.common.response.PaginationMeta;
import com.mamikos.kostapi.inquiry.service.AvailabilityInquiryService;
import com.mamikos.kostapi.inquiry.web.dto.CreateInquiryRequest;
import com.mamikos.kostapi.inquiry.web.dto.InquiryCreatedResponse;
import com.mamikos.kostapi.inquiry.web.dto.InquiryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "Availability Inquiries",
        description = "Ask about room availability for 5 credits, and see your own history")
public class AvailabilityInquiryController {

    private final AvailabilityInquiryService inquiryService;

    public AvailabilityInquiryController(AvailabilityInquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping("/api/v1/kosts/{id}/availability-inquiries")
    @PreAuthorize("hasAnyRole('REGULAR', 'PREMIUM')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryCreatedResponse> create(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable("id") Long kostId,
            @Valid @RequestBody CreateInquiryRequest request) {
        return ApiResponse.of("Availability inquiry submitted", inquiryService.create(principal.id(), kostId, request));
    }

    @GetMapping("/api/v1/me/inquiries")
    @PreAuthorize("hasAnyRole('REGULAR', 'PREMIUM')")
    public ApiResponse<List<InquiryResponse>> myInquiries(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        int safePerPage = Math.min(Math.max(perPage, 1), 50);
        var pageable = PageRequest.of(Math.max(page - 1, 0), safePerPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InquiryResponse> result = inquiryService.myInquiries(principal.id(), pageable);
        return ApiResponse.of(
                "Inquiry history retrieved", result.getContent(), Map.of("pagination", PaginationMeta.of(result)));
    }
}
