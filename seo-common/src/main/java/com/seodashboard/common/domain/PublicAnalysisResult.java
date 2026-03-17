package com.seodashboard.common.domain;

import java.math.BigDecimal;

public record PublicAnalysisResult(
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
        String headingStructure,
        String issues,
        String linkList,
        String metaTags
) {
}
