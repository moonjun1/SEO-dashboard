package com.seodashboard.api.analysis.dto;

import com.seodashboard.common.domain.ContentAnalysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContentAnalysisListResponse(
        Long id,
        Long siteId,
        String title,
        String targetKeywords,
        BigDecimal seoScore,
        BigDecimal readabilityScore,
        String aiProvider,
        String status,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static ContentAnalysisListResponse from(ContentAnalysis analysis) {
        return new ContentAnalysisListResponse(
                analysis.getId(),
                analysis.getSite() != null ? analysis.getSite().getId() : null,
                analysis.getTitle(),
                analysis.getTargetKeywords(),
                analysis.getSeoScore(),
                analysis.getReadabilityScore(),
                analysis.getAiProvider(),
                analysis.getStatus(),
                analysis.getCompletedAt(),
                analysis.getCreatedAt()
        );
    }
}
