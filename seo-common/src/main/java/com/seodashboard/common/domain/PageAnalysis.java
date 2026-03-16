package com.seodashboard.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "page_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageAnalysis extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_result_id", nullable = false, unique = true)
    private CrawlResult crawlResult;

    @Column(name = "seo_score", precision = 5, scale = 2)
    private BigDecimal seoScore;

    @Column(name = "title_score", precision = 5, scale = 2)
    private BigDecimal titleScore;

    @Column(name = "title_length")
    private Integer titleLength;

    @Column(name = "meta_description_score", precision = 5, scale = 2)
    private BigDecimal metaDescriptionScore;

    @Column(name = "meta_description_length")
    private Integer metaDescriptionLength;

    @Column(name = "heading_score", precision = 5, scale = 2)
    private BigDecimal headingScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "heading_structure", columnDefinition = "jsonb")
    private String headingStructure;

    @Column(name = "image_score", precision = 5, scale = 2)
    private BigDecimal imageScore;

    @Column(name = "images_total")
    private int imagesTotal;

    @Column(name = "images_without_alt")
    private int imagesWithoutAlt;

    @Column(name = "link_score", precision = 5, scale = 2)
    private BigDecimal linkScore;

    @Column(name = "internal_links_count")
    private int internalLinksCount;

    @Column(name = "external_links_count")
    private int externalLinksCount;

    @Column(name = "broken_links_count")
    private int brokenLinksCount;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "has_og_tags")
    private Boolean hasOgTags;

    @Column(name = "has_twitter_cards")
    private Boolean hasTwitterCards;

    @Column(name = "has_structured_data")
    private Boolean hasStructuredData;

    @Column(name = "has_sitemap")
    private Boolean hasSitemap;

    @Column(name = "has_robots_txt")
    private Boolean hasRobotsTxt;

    @Column(name = "is_mobile_friendly")
    private Boolean isMobileFriendly;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String issues;

    @Builder
    public PageAnalysis(CrawlResult crawlResult, BigDecimal seoScore, BigDecimal titleScore,
                         Integer titleLength, BigDecimal metaDescriptionScore,
                         Integer metaDescriptionLength, BigDecimal headingScore,
                         String headingStructure, BigDecimal imageScore, int imagesTotal,
                         int imagesWithoutAlt, BigDecimal linkScore, int internalLinksCount,
                         int externalLinksCount, int brokenLinksCount, BigDecimal performanceScore,
                         Boolean hasOgTags, Boolean hasTwitterCards, Boolean hasStructuredData,
                         Boolean hasSitemap, Boolean hasRobotsTxt, Boolean isMobileFriendly,
                         String issues) {
        this.crawlResult = crawlResult;
        this.seoScore = seoScore;
        this.titleScore = titleScore;
        this.titleLength = titleLength;
        this.metaDescriptionScore = metaDescriptionScore;
        this.metaDescriptionLength = metaDescriptionLength;
        this.headingScore = headingScore;
        this.headingStructure = headingStructure;
        this.imageScore = imageScore;
        this.imagesTotal = imagesTotal;
        this.imagesWithoutAlt = imagesWithoutAlt;
        this.linkScore = linkScore;
        this.internalLinksCount = internalLinksCount;
        this.externalLinksCount = externalLinksCount;
        this.brokenLinksCount = brokenLinksCount;
        this.performanceScore = performanceScore;
        this.hasOgTags = hasOgTags;
        this.hasTwitterCards = hasTwitterCards;
        this.hasStructuredData = hasStructuredData;
        this.hasSitemap = hasSitemap;
        this.hasRobotsTxt = hasRobotsTxt;
        this.isMobileFriendly = isMobileFriendly;
        this.issues = issues;
    }
}
