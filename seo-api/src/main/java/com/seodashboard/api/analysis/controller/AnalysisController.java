package com.seodashboard.api.analysis.controller;

import com.seodashboard.api.analysis.dto.ContentAnalysisListResponse;
import com.seodashboard.api.analysis.dto.ContentAnalysisRequest;
import com.seodashboard.api.analysis.dto.ContentAnalysisResponse;
import com.seodashboard.api.analysis.dto.MetaGenerateRequest;
import com.seodashboard.api.analysis.dto.MetaGenerateResponse;
import com.seodashboard.api.analysis.service.AnalysisService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Content Analysis", description = "AI-powered content analysis and optimization API")
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "Request content analysis",
            description = "Submit content for async SEO analysis. Returns 202 Accepted with analysis ID.")
    @PostMapping("/content")
    public ResponseEntity<ApiResponse<ContentAnalysisResponse>> requestAnalysis(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ContentAnalysisRequest request
    ) {
        ContentAnalysisResponse response = analysisService.requestAnalysis(user, request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Content analysis started"));
    }

    @Operation(summary = "Get analysis result", description = "Retrieve content analysis result by ID")
    @GetMapping("/content/{analysisId}")
    public ResponseEntity<ApiResponse<ContentAnalysisResponse>> getAnalysis(
            @AuthenticationPrincipal User user,
            @PathVariable Long analysisId
    ) {
        ContentAnalysisResponse response = analysisService.getAnalysis(analysisId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "List analyses", description = "Get paginated list of content analyses with optional filters")
    @GetMapping("/content")
    public ResponseEntity<ApiResponse<PageResponse<ContentAnalysisListResponse>>> listAnalyses(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ContentAnalysisListResponse> response = analysisService.listAnalyses(
                user.getId(), siteId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Generate meta tags",
            description = "Generate meta title and description suggestions synchronously")
    @PostMapping("/meta-generate")
    public ResponseEntity<ApiResponse<MetaGenerateResponse>> generateMeta(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody MetaGenerateRequest request
    ) {
        MetaGenerateResponse response = analysisService.generateMeta(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
