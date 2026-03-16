package com.seodashboard.api.crawl.controller;

import com.seodashboard.api.crawl.dto.CrawlJobListResponse;
import com.seodashboard.api.crawl.dto.CrawlJobResponse;
import com.seodashboard.api.crawl.dto.CrawlResultResponse;
import com.seodashboard.api.crawl.dto.CrawlStartRequest;
import com.seodashboard.api.crawl.service.CrawlService;
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

@Tag(name = "Crawl", description = "Site crawling and SEO analysis API")
@RestController
@RequestMapping("/api/v1/sites/{siteId}/crawl")
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlService crawlService;

    @Operation(summary = "Start crawl", description = "Start a new crawl job for the site")
    @PostMapping
    public ResponseEntity<ApiResponse<CrawlJobResponse>> startCrawl(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody(required = false) CrawlStartRequest request
    ) {
        if (request == null) {
            request = new CrawlStartRequest(null, null);
        }
        CrawlJobResponse response = crawlService.startCrawl(siteId, user.getId(), request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Crawl job started"));
    }

    @Operation(summary = "Get crawl jobs", description = "Get paginated list of crawl jobs for the site")
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<PageResponse<CrawlJobListResponse>>> getCrawlJobs(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CrawlJobListResponse> response = crawlService.getCrawlJobs(siteId, user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get crawl job", description = "Get crawl job details by ID")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<CrawlJobResponse>> getCrawlJob(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long jobId
    ) {
        CrawlJobResponse response = crawlService.getCrawlJob(siteId, user.getId(), jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Cancel crawl", description = "Cancel a running or pending crawl job")
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<ApiResponse<CrawlJobResponse>> cancelCrawl(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long jobId
    ) {
        CrawlJobResponse response = crawlService.cancelCrawl(siteId, user.getId(), jobId);
        return ResponseEntity.ok(ApiResponse.success(response, "Crawl job cancelled"));
    }

    @Operation(summary = "Get crawl results", description = "Get paginated crawl results with SEO analysis")
    @GetMapping("/jobs/{jobId}/results")
    public ResponseEntity<ApiResponse<PageResponse<CrawlResultResponse>>> getCrawlResults(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long jobId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<CrawlResultResponse> response = crawlService.getCrawlResults(
                siteId, user.getId(), jobId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
