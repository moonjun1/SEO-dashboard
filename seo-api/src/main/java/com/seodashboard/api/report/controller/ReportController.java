package com.seodashboard.api.report.controller;

import com.seodashboard.api.report.dto.ReportCreateRequest;
import com.seodashboard.api.report.dto.ReportListResponse;
import com.seodashboard.api.report.dto.ReportResponse;
import com.seodashboard.api.report.service.ReportService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "SEO report generation API")
@RestController
@RequestMapping("/api/v1/sites/{siteId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Generate report", description = "Generate a new SEO report for the site")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        ReportResponse response = reportService.generateReport(siteId, user.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Report generated successfully"));
    }

    @Operation(summary = "Get reports", description = "Get paginated list of reports for the site")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportListResponse>>> getReports(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ReportListResponse> response = reportService.getReports(siteId, user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get report", description = "Get report details by ID")
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long reportId
    ) {
        ReportResponse response = reportService.getReport(siteId, user.getId(), reportId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
