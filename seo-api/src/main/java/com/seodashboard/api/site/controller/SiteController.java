package com.seodashboard.api.site.controller;

import com.seodashboard.api.site.dto.SiteCreateRequest;
import com.seodashboard.api.site.dto.SiteListResponse;
import com.seodashboard.api.site.dto.SiteResponse;
import com.seodashboard.api.site.dto.SiteUpdateRequest;
import com.seodashboard.api.site.service.SiteService;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.dto.ApiResponse;
import com.seodashboard.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sites", description = "Site management API")
@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @Operation(summary = "Create site", description = "Register a new site for SEO monitoring")
    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponse>> createSite(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SiteCreateRequest request
    ) {
        SiteResponse response = siteService.createSite(user, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Site created successfully"));
    }

    @Operation(summary = "Get sites", description = "Get paginated list of user's sites")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SiteListResponse>>> getSites(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SiteListResponse> response = siteService.getSites(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get site", description = "Get site details by ID")
    @GetMapping("/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> getSite(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId
    ) {
        SiteResponse response = siteService.getSite(siteId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update site", description = "Update site information")
    @PutMapping("/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> updateSite(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody SiteUpdateRequest request
    ) {
        SiteResponse response = siteService.updateSite(siteId, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Site updated successfully"));
    }

    @Operation(summary = "Delete site", description = "Soft delete a site")
    @DeleteMapping("/{siteId}")
    public ResponseEntity<ApiResponse<Void>> deleteSite(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId
    ) {
        siteService.deleteSite(siteId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Site deleted successfully"));
    }
}
