package com.seodashboard.api.dashboard.controller;

import com.seodashboard.api.dashboard.dto.DashboardResponse;
import com.seodashboard.api.dashboard.dto.SiteDashboardResponse;
import com.seodashboard.api.dashboard.dto.SiteStatsResponse;
import com.seodashboard.api.dashboard.service.DashboardService;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "SEO dashboard and analytics API")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard", description = "Get comprehensive dashboard with all sites overview")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal User user
    ) {
        DashboardResponse response = dashboardService.getDashboard(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get site dashboard", description = "Get detailed dashboard for a specific site")
    @GetMapping("/sites/{siteId}")
    public ResponseEntity<ApiResponse<SiteDashboardResponse>> getSiteDashboard(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId
    ) {
        SiteDashboardResponse response = dashboardService.getSiteDashboard(siteId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get site stats", description = "Get site statistics with keyword ranking distribution")
    @GetMapping("/sites/{siteId}/stats")
    public ResponseEntity<ApiResponse<SiteStatsResponse>> getSiteStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @RequestParam(defaultValue = "30d") String period
    ) {
        SiteStatsResponse response = dashboardService.getSiteStats(siteId, user.getId(), period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
