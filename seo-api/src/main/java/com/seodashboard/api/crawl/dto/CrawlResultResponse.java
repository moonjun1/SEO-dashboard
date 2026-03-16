package com.seodashboard.api.crawl.dto;

import com.seodashboard.common.domain.CrawlResult;
import com.seodashboard.common.domain.PageAnalysis;

import java.time.LocalDateTime;

public record CrawlResultResponse(
        Long id,
        String url,
        Integer statusCode,
        String contentType,
        Long contentLength,
        Integer responseTimeMs,
        String title,
        String metaDescription,
        String canonicalUrl,
        int depth,
        boolean isInternal,
        String redirectUrl,
        PageAnalysisResponse analysis,
        LocalDateTime createdAt
) {

    public static CrawlResultResponse from(CrawlResult crawlResult, PageAnalysis analysis) {
        return new CrawlResultResponse(
                crawlResult.getId(),
                crawlResult.getUrl(),
                crawlResult.getStatusCode(),
                crawlResult.getContentType(),
                crawlResult.getContentLength(),
                crawlResult.getResponseTimeMs(),
                crawlResult.getTitle(),
                crawlResult.getMetaDescription(),
                crawlResult.getCanonicalUrl(),
                crawlResult.getDepth(),
                crawlResult.isInternal(),
                crawlResult.getRedirectUrl(),
                analysis != null ? PageAnalysisResponse.from(analysis) : null,
                crawlResult.getCreatedAt()
        );
    }
}
