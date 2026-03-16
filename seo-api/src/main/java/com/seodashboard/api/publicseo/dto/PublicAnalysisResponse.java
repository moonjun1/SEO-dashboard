package com.seodashboard.api.publicseo.dto;

import com.seodashboard.common.domain.PublicAnalysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicAnalysisResponse(
        Long id,
        String url,
        String domain,
        BigDecimal seoScore,
        BigDecimal titleScore,
        BigDecimal metaDescriptionScore,
        BigDecimal headingScore,
        BigDecimal imageScore,
        BigDecimal linkScore,
        BigDecimal performanceScore,
        String title,
        String metaDescription,
        String canonicalUrl,
        Integer responseTimeMs,
        Integer contentLength,
        int totalImages,
        int imagesWithoutAlt,
        int internalLinks,
        int externalLinks,
        int brokenLinks,
        int totalHeadings,
        boolean hasOgTags,
        boolean hasTwitterCards,
        boolean hasViewport,
        boolean hasFavicon,
        boolean hasRobotsTxt,
        boolean hasSitemap,
        boolean hasHttps,
        Object headingStructure,
        Object issues,
        Object linkList,
        Object metaTags,
        String status,
        String errorMessage,
        LocalDateTime createdAt
) {

    public static PublicAnalysisResponse from(PublicAnalysis entity, Object headingStructure,
                                               Object issues, Object linkList, Object metaTags) {
        return new PublicAnalysisResponse(
                entity.getId(),
                entity.getUrl(),
                entity.getDomain(),
                entity.getSeoScore(),
                entity.getTitleScore(),
                entity.getMetaDescriptionScore(),
                entity.getHeadingScore(),
                entity.getImageScore(),
                entity.getLinkScore(),
                entity.getPerformanceScore(),
                entity.getTitle(),
                entity.getMetaDescription(),
                entity.getCanonicalUrl(),
                entity.getResponseTimeMs(),
                entity.getContentLength(),
                entity.getTotalImages(),
                entity.getImagesWithoutAlt(),
                entity.getInternalLinks(),
                entity.getExternalLinks(),
                entity.getBrokenLinks(),
                entity.getTotalHeadings(),
                entity.isHasOgTags(),
                entity.isHasTwitterCards(),
                entity.isHasViewport(),
                entity.isHasFavicon(),
                entity.isHasRobotsTxt(),
                entity.isHasSitemap(),
                entity.isHasHttps(),
                headingStructure,
                issues,
                linkList,
                metaTags,
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }
}
