package com.seodashboard.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "public_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicAnalysis extends BaseEntity {

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 500)
    private String domain;

    @Column(name = "seo_score", precision = 5, scale = 2)
    private BigDecimal seoScore;

    @Column(name = "title_score", precision = 5, scale = 2)
    private BigDecimal titleScore;

    @Column(name = "meta_description_score", precision = 5, scale = 2)
    private BigDecimal metaDescriptionScore;

    @Column(name = "heading_score", precision = 5, scale = 2)
    private BigDecimal headingScore;

    @Column(name = "image_score", precision = 5, scale = 2)
    private BigDecimal imageScore;

    @Column(name = "link_score", precision = 5, scale = 2)
    private BigDecimal linkScore;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    // Detail data
    private String title;

    @Column(columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "content_length")
    private Integer contentLength;

    // Counts
    @Column(name = "total_images")
    private int totalImages;

    @Column(name = "images_without_alt")
    private int imagesWithoutAlt;

    @Column(name = "internal_links")
    private int internalLinks;

    @Column(name = "external_links")
    private int externalLinks;

    @Column(name = "broken_links")
    private int brokenLinks;

    @Column(name = "total_headings")
    private int totalHeadings;

    // Boolean checks
    @Column(name = "has_og_tags")
    private boolean hasOgTags;

    @Column(name = "has_twitter_cards")
    private boolean hasTwitterCards;

    @Column(name = "has_viewport")
    private boolean hasViewport;

    @Column(name = "has_favicon")
    private boolean hasFavicon;

    @Column(name = "has_robots_txt")
    private boolean hasRobotsTxt;

    @Column(name = "has_sitemap")
    private boolean hasSitemap;

    @Column(name = "has_https")
    private boolean hasHttps;

    // JSONB details
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "heading_structure", columnDefinition = "jsonb")
    private String headingStructure;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String issues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "link_list", columnDefinition = "jsonb")
    private String linkList;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta_tags", columnDefinition = "jsonb")
    private String metaTags;

    // Status
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Builder
    public PublicAnalysis(String url, String domain, BigDecimal seoScore, BigDecimal titleScore,
                          BigDecimal metaDescriptionScore, BigDecimal headingScore, BigDecimal imageScore,
                          BigDecimal linkScore, BigDecimal performanceScore, String title,
                          String metaDescription, String canonicalUrl, Integer responseTimeMs,
                          Integer contentLength, int totalImages, int imagesWithoutAlt,
                          int internalLinks, int externalLinks, int brokenLinks, int totalHeadings,
                          boolean hasOgTags, boolean hasTwitterCards, boolean hasViewport,
                          boolean hasFavicon, boolean hasRobotsTxt, boolean hasSitemap, boolean hasHttps,
                          String headingStructure, String issues, String linkList, String metaTags,
                          String status, String errorMessage) {
        this.url = url;
        this.domain = domain;
        this.seoScore = seoScore;
        this.titleScore = titleScore;
        this.metaDescriptionScore = metaDescriptionScore;
        this.headingScore = headingScore;
        this.imageScore = imageScore;
        this.linkScore = linkScore;
        this.performanceScore = performanceScore;
        this.title = title;
        this.metaDescription = metaDescription;
        this.canonicalUrl = canonicalUrl;
        this.responseTimeMs = responseTimeMs;
        this.contentLength = contentLength;
        this.totalImages = totalImages;
        this.imagesWithoutAlt = imagesWithoutAlt;
        this.internalLinks = internalLinks;
        this.externalLinks = externalLinks;
        this.brokenLinks = brokenLinks;
        this.totalHeadings = totalHeadings;
        this.hasOgTags = hasOgTags;
        this.hasTwitterCards = hasTwitterCards;
        this.hasViewport = hasViewport;
        this.hasFavicon = hasFavicon;
        this.hasRobotsTxt = hasRobotsTxt;
        this.hasSitemap = hasSitemap;
        this.hasHttps = hasHttps;
        this.headingStructure = headingStructure;
        this.issues = issues;
        this.linkList = linkList;
        this.metaTags = metaTags;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public void markCompleted(BigDecimal seoScore, BigDecimal titleScore, BigDecimal metaDescriptionScore,
                               BigDecimal headingScore, BigDecimal imageScore, BigDecimal linkScore,
                               BigDecimal performanceScore, String title, String metaDescription,
                               String canonicalUrl, Integer responseTimeMs, Integer contentLength,
                               int totalImages, int imagesWithoutAlt, int internalLinks,
                               int externalLinks, int brokenLinks, int totalHeadings,
                               boolean hasOgTags, boolean hasTwitterCards, boolean hasViewport,
                               boolean hasFavicon, boolean hasRobotsTxt, boolean hasSitemap,
                               boolean hasHttps, String headingStructure, String issues,
                               String linkList, String metaTags) {
        this.seoScore = seoScore;
        this.titleScore = titleScore;
        this.metaDescriptionScore = metaDescriptionScore;
        this.headingScore = headingScore;
        this.imageScore = imageScore;
        this.linkScore = linkScore;
        this.performanceScore = performanceScore;
        this.title = title;
        this.metaDescription = metaDescription;
        this.canonicalUrl = canonicalUrl;
        this.responseTimeMs = responseTimeMs;
        this.contentLength = contentLength;
        this.totalImages = totalImages;
        this.imagesWithoutAlt = imagesWithoutAlt;
        this.internalLinks = internalLinks;
        this.externalLinks = externalLinks;
        this.brokenLinks = brokenLinks;
        this.totalHeadings = totalHeadings;
        this.hasOgTags = hasOgTags;
        this.hasTwitterCards = hasTwitterCards;
        this.hasViewport = hasViewport;
        this.hasFavicon = hasFavicon;
        this.hasRobotsTxt = hasRobotsTxt;
        this.hasSitemap = hasSitemap;
        this.hasHttps = hasHttps;
        this.headingStructure = headingStructure;
        this.issues = issues;
        this.linkList = linkList;
        this.metaTags = metaTags;
        this.status = "COMPLETED";
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }
}
