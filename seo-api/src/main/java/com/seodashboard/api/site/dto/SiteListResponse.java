package com.seodashboard.api.site.dto;

import com.seodashboard.common.domain.Site;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SiteListResponse(
        Long id,
        String url,
        String name,
        BigDecimal seoScore,
        LocalDateTime lastCrawledAt,
        boolean isActive
) {

    public static SiteListResponse from(Site site) {
        return new SiteListResponse(
                site.getId(),
                site.getUrl(),
                site.getName(),
                site.getSeoScore(),
                site.getLastCrawledAt(),
                site.isActive()
        );
    }
}
