package com.seodashboard.api.publicseo.controller;

import com.seodashboard.api.publicseo.dto.PublicAnalysisListResponse;
import com.seodashboard.api.publicseo.dto.PublicAnalysisResponse;
import com.seodashboard.api.publicseo.dto.PublicAnalyzeRequest;
import com.seodashboard.api.publicseo.service.PublicSeoService;
import com.seodashboard.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Public SEO", description = "Public SEO analysis API (no authentication required)")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSeoController {

    private final PublicSeoService publicSeoService;

    @Operation(summary = "Analyze URL", description = "Perform SEO analysis on a given URL (synchronous)")
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<PublicAnalysisResponse>> analyze(
            @Valid @RequestBody PublicAnalyzeRequest request
    ) {
        PublicAnalysisResponse response = publicSeoService.analyze(request.url());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get analysis result", description = "Get a specific analysis result by ID")
    @GetMapping("/analyze/{id}")
    public ResponseEntity<ApiResponse<PublicAnalysisResponse>> getAnalysis(
            @PathVariable Long id
    ) {
        PublicAnalysisResponse response = publicSeoService.getAnalysis(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Recent analyses", description = "Get the 20 most recent analyses")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PublicAnalysisListResponse>>> getRecentAnalyses() {
        List<PublicAnalysisListResponse> response = publicSeoService.getRecentAnalyses();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "SEO score ranking", description = "Get top 50 analyses by SEO score")
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<PublicAnalysisListResponse>>> getRanking() {
        List<PublicAnalysisListResponse> response = publicSeoService.getRanking();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Domain history", description = "Get analysis history for a specific domain")
    @GetMapping("/domain/{domain}")
    public ResponseEntity<ApiResponse<List<PublicAnalysisListResponse>>> getDomainHistory(
            @PathVariable String domain
    ) {
        List<PublicAnalysisListResponse> response = publicSeoService.getDomainHistory(domain);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
