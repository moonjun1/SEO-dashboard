package com.seodashboard.api.crawl.dto;

import com.seodashboard.common.domain.PageAnalysis;

import java.math.BigDecimal;

public record PageAnalysisResponse(
        Long id,
        BigDecimal seoScore,
        BigDecimal titleScore,
        Integer titleLength,
        BigDecimal metaDescriptionScore,
        Integer metaDescriptionLength,
        BigDecimal headingScore,
        String headingStructure,
        BigDecimal imageScore,
        int imagesTotal,
        int imagesWithoutAlt,
        BigDecimal linkScore,
        int internalLinksCount,
        int externalLinksCount,
        int brokenLinksCount,
        BigDecimal performanceScore,
        Boolean hasOgTags,
        Boolean hasTwitterCards,
        Boolean hasStructuredData,
        Boolean hasSitemap,
        Boolean hasRobotsTxt,
        Boolean isMobileFriendly,
        String issues
) {

    public static PageAnalysisResponse from(PageAnalysis analysis) {
        return new PageAnalysisResponse(
                analysis.getId(),
                analysis.getSeoScore(),
                analysis.getTitleScore(),
                analysis.getTitleLength(),
                analysis.getMetaDescriptionScore(),
                analysis.getMetaDescriptionLength(),
                analysis.getHeadingScore(),
                analysis.getHeadingStructure(),
                analysis.getImageScore(),
                analysis.getImagesTotal(),
                analysis.getImagesWithoutAlt(),
                analysis.getLinkScore(),
                analysis.getInternalLinksCount(),
                analysis.getExternalLinksCount(),
                analysis.getBrokenLinksCount(),
                analysis.getPerformanceScore(),
                analysis.getHasOgTags(),
                analysis.getHasTwitterCards(),
                analysis.getHasStructuredData(),
                analysis.getHasSitemap(),
                analysis.getHasRobotsTxt(),
                analysis.getIsMobileFriendly(),
                analysis.getIssues()
        );
    }
}
