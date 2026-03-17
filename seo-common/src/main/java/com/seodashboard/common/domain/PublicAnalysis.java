package com.seodashboard.common.domain;

import com.seodashboard.common.domain.enums.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Builder
    public PublicAnalysis(String url, String domain, AnalysisStatus status, String errorMessage) {
        this.url = url;
        this.domain = domain;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public void markCompleted(PublicAnalysisResult result) {
        this.seoScore = result.seoScore();
        this.titleScore = result.titleScore();
        this.metaDescriptionScore = result.metaDescriptionScore();
        this.headingScore = result.headingScore();
        this.imageScore = result.imageScore();
        this.linkScore = result.linkScore();
        this.performanceScore = result.performanceScore();
        this.title = result.title();
        this.metaDescription = result.metaDescription();
        this.canonicalUrl = result.canonicalUrl();
        this.responseTimeMs = result.responseTimeMs();
        this.contentLength = result.contentLength();
        this.totalImages = result.totalImages();
        this.imagesWithoutAlt = result.imagesWithoutAlt();
        this.internalLinks = result.internalLinks();
        this.externalLinks = result.externalLinks();
        this.brokenLinks = result.brokenLinks();
        this.totalHeadings = result.totalHeadings();
        this.hasOgTags = result.hasOgTags();
        this.hasTwitterCards = result.hasTwitterCards();
        this.hasViewport = result.hasViewport();
        this.hasFavicon = result.hasFavicon();
        this.hasRobotsTxt = result.hasRobotsTxt();
        this.hasSitemap = result.hasSitemap();
        this.hasHttps = result.hasHttps();
        this.headingStructure = result.headingStructure();
        this.issues = result.issues();
        this.linkList = result.linkList();
        this.metaTags = result.metaTags();
        this.status = AnalysisStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
