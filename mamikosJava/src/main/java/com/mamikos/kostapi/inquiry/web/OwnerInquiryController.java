package com.mamikos.kostapi.inquiry.web;

import com.mamikos.kostapi.auth.security.SecurityUser;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.common.response.PaginationMeta;
import com.mamikos.kostapi.inquiry.service.AvailabilityInquiryService;
import com.mamikos.kostapi.inquiry.web.dto.InquiryResponse;
import com.mamikos.kostapi.inquiry.web.dto.ReplyInquiryRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/inquiries")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Owner Inquiries", description = "Inquiries received on the caller's own kosts")
public class OwnerInquiryController {

    private final AvailabilityInquiryService inquiryService;

    public OwnerInquiryController(AvailabilityInquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public ApiResponse<List<InquiryResponse>> list(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        int safePerPage = Math.min(Math.max(perPage, 1), 50);
        var pageable = PageRequest.of(Math.max(page - 1, 0), safePerPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InquiryResponse> result = inquiryService.forOwner(principal.id(), status, pageable);
        return ApiResponse.of(
                "Inquiries retrieved", result.getContent(), Map.of("pagination", PaginationMeta.of(result)));
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<InquiryResponse> reply(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long id,
            @Valid @RequestBody ReplyInquiryRequest request) {
        return ApiResponse.of("Reply submitted", inquiryService.reply(principal.id(), id, request));
    }
}
