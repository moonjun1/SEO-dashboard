package com.seodashboard.crawler.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CrawlPageResult {

    // Fetch results
    private final String url;
    private final int statusCode;
    private final String contentType;
    private final Long contentLength;
    private final int responseTimeMs;
    private final int depth;
    private final boolean isInternal;
    private final String redirectUrl;

    // Parsed content
    private final String title;
    private final String metaDescription;
    private final String canonicalUrl;
    private final List<String> internalLinks;
    private final List<String> externalLinks;

    // Analysis results
    private final BigDecimal seoScore;
    private final BigDecimal titleScore;
    private final Integer titleLength;
    private final BigDecimal metaDescriptionScore;
    private final Integer metaDescriptionLength;
    private final BigDecimal headingScore;
    private final String headingStructure;
    private final BigDecimal imageScore;
    private final int imagesTotal;
    private final int imagesWithoutAlt;
    private final BigDecimal linkScore;
    private final int internalLinksCount;
    private final int externalLinksCount;
    private final int brokenLinksCount;
    private final BigDecimal performanceScore;
    private final Boolean hasOgTags;
    private final Boolean hasTwitterCards;
    private final Boolean hasStructuredData;
    private final Boolean hasSitemap;
    private final Boolean hasRobotsTxt;
    private final Boolean isMobileFriendly;
    private final List<SeoIssue> issues;
}
