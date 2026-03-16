package com.seodashboard.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentAnalysis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "target_keywords", length = 1000)
    private String targetKeywords;

    @Column(name = "seo_score", precision = 5, scale = 2)
    private BigDecimal seoScore;

    @Column(name = "readability_score", precision = 5, scale = 2)
    private BigDecimal readabilityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyword_density", columnDefinition = "jsonb")
    private String keywordDensity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure_analysis", columnDefinition = "jsonb")
    private String structureAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String suggestions;

    @Column(name = "generated_meta_title", length = 200)
    private String generatedMetaTitle;

    @Column(name = "generated_meta_description", length = 500)
    private String generatedMetaDescription;

    @Column(name = "ai_provider", length = 20)
    private String aiProvider;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public ContentAnalysis(Site site, User user, String title, String content, String targetKeywords) {
        this.site = site;
        this.user = user;
        this.title = title;
        this.content = content;
        this.targetKeywords = targetKeywords;
        this.status = "PENDING";
    }

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void markCompleted(BigDecimal seoScore, BigDecimal readabilityScore,
                              String keywordDensity, String structureAnalysis,
                              String suggestions, String generatedMetaTitle,
                              String generatedMetaDescription,
                              String aiProvider, String aiModel) {
        this.status = "COMPLETED";
        this.seoScore = seoScore;
        this.readabilityScore = readabilityScore;
        this.keywordDensity = keywordDensity;
        this.structureAnalysis = structureAnalysis;
        this.suggestions = suggestions;
        this.generatedMetaTitle = generatedMetaTitle;
        this.generatedMetaDescription = generatedMetaDescription;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }
}
