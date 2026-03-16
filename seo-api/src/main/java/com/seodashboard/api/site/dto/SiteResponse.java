package com.seodashboard.api.site.dto;

import com.seodashboard.common.domain.Site;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SiteResponse(
        Long id,
        String url,
        String name,
        String description,
        BigDecimal seoScore,
        LocalDateTime lastCrawledAt,
        int crawlIntervalHours,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SiteResponse from(Site site) {
        return new SiteResponse(
                site.getId(),
                site.getUrl(),
                site.getName(),
                site.getDescription(),
                site.getSeoScore(),
                site.getLastCrawledAt(),
                site.getCrawlIntervalHours(),
                site.isActive(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
