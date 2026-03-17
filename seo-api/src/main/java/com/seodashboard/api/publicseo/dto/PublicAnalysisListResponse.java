package com.seodashboard.api.publicseo.dto;

import com.seodashboard.common.domain.PublicAnalysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicAnalysisListResponse(
        Long id,
        String url,
        String domain,
        BigDecimal seoScore,
        String title,
        Integer responseTimeMs,
        String status,
        LocalDateTime createdAt
) {

    public static PublicAnalysisListResponse from(PublicAnalysis entity) {
        return new PublicAnalysisListResponse(
                entity.getId(),
                entity.getUrl(),
                entity.getDomain(),
                entity.getSeoScore(),
                entity.getTitle(),
                entity.getResponseTimeMs(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}
