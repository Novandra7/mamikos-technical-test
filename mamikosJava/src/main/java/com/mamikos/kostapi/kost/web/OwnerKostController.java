package com.mamikos.kostapi.kost.web;

import com.mamikos.kostapi.auth.security.SecurityUser;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.common.response.PaginationMeta;
import com.mamikos.kostapi.kost.service.KostService;
import com.mamikos.kostapi.kost.web.dto.request.CreateKostRequest;
import com.mamikos.kostapi.kost.web.dto.request.PatchKostRequest;
import com.mamikos.kostapi.kost.web.dto.request.UpdateKostRequest;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/kosts")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Owner Kosts", description = "Create, list, update, and delete kosts owned by the caller")
public class OwnerKostController {

    private final KostService kostService;

    public OwnerKostController(KostService kostService) {
        this.kostService = kostService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnerKostResponse> create(
            @AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody CreateKostRequest request) {
        return ApiResponse.of("Kost created", kostService.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<OwnerKostResponse>> list(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        int safePerPage = Math.min(Math.max(perPage, 1), 50);
        var pageable = PageRequest.of(Math.max(page - 1, 0), safePerPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OwnerKostResponse> result = kostService.listForOwner(principal.id(), includeDeleted, pageable);
        return ApiResponse.of(
                "Kost list retrieved", result.getContent(), Map.of("pagination", PaginationMeta.of(result)));
    }

    @GetMapping("/{id}")
    public ApiResponse<OwnerKostResponse> get(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ApiResponse.of("Kost retrieved", kostService.getOwned(principal.id(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<OwnerKostResponse> replace(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateKostRequest request) {
        return ApiResponse.of("Kost updated", kostService.replace(principal.id(), id, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<OwnerKostResponse> patch(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long id,
            @Valid @RequestBody PatchKostRequest request) {
        return ApiResponse.of("Kost updated", kostService.patch(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        kostService.softDelete(principal.id(), id);
        return ApiResponse.of("Kost deleted", null);
    }
}
