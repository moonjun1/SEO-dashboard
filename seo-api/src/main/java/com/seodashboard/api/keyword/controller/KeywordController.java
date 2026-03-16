package com.seodashboard.api.keyword.controller;

import com.seodashboard.api.keyword.dto.KeywordBatchCreateRequest;
import com.seodashboard.api.keyword.dto.KeywordCreateRequest;
import com.seodashboard.api.keyword.dto.KeywordListResponse;
import com.seodashboard.api.keyword.dto.KeywordResponse;
import com.seodashboard.api.keyword.dto.KeywordUpdateRequest;
import com.seodashboard.api.keyword.dto.RankingHistoryResponse;
import com.seodashboard.api.keyword.service.KeywordService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Keywords", description = "Keyword ranking tracking API")
@RestController
@RequestMapping("/api/v1/sites/{siteId}/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;

    @Operation(summary = "Create keyword", description = "Register a new keyword for ranking tracking")
    @PostMapping
    public ResponseEntity<ApiResponse<KeywordResponse>> createKeyword(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody KeywordCreateRequest request
    ) {
        KeywordResponse response = keywordService.createKeyword(siteId, user.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Keyword created successfully"));
    }

    @Operation(summary = "Batch create keywords", description = "Register multiple keywords at once")
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> batchCreateKeywords(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody KeywordBatchCreateRequest request
    ) {
        List<KeywordResponse> response = keywordService.batchCreateKeywords(siteId, user.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Keywords created successfully"));
    }

    @Operation(summary = "Get keywords", description = "Get paginated list of keywords for the site")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<KeywordListResponse>>> getKeywords(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<KeywordListResponse> response = keywordService.getKeywords(
                siteId, user.getId(), isActive, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update keyword", description = "Update keyword target URL or active status")
    @PutMapping("/{keywordId}")
    public ResponseEntity<ApiResponse<KeywordResponse>> updateKeyword(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long keywordId,
            @Valid @RequestBody KeywordUpdateRequest request
    ) {
        KeywordResponse response = keywordService.updateKeyword(siteId, user.getId(), keywordId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Keyword updated successfully"));
    }

    @Operation(summary = "Delete keyword", description = "Delete a keyword and its ranking history")
    @DeleteMapping("/{keywordId}")
    public ResponseEntity<ApiResponse<Void>> deleteKeyword(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long keywordId
    ) {
        keywordService.deleteKeyword(siteId, user.getId(), keywordId);
        return ResponseEntity.ok(ApiResponse.success(null, "Keyword deleted successfully"));
    }

    @Operation(summary = "Get ranking history", description = "Get keyword ranking history for a period")
    @GetMapping("/{keywordId}/rankings")
    public ResponseEntity<ApiResponse<RankingHistoryResponse>> getRankingHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long keywordId,
            @RequestParam(defaultValue = "30d") String period
    ) {
        RankingHistoryResponse response = keywordService.getRankingHistory(
                siteId, user.getId(), keywordId, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Collect rankings", description = "Manually trigger ranking collection for all active keywords")
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<Integer>> collectRankings(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId
    ) {
        int collected = keywordService.collectRankings(siteId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(collected,
                "Rankings collected for " + collected + " keywords"));
    }
}
