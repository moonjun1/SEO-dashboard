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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Site extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "seo_score", precision = 5, scale = 2)
    private BigDecimal seoScore;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    @Column(name = "crawl_interval_hours", nullable = false)
    private int crawlIntervalHours = 168;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public Site(User user, String url, String name, String description, int crawlIntervalHours) {
        this.user = user;
        this.url = url;
        this.name = name;
        this.description = description;
        this.crawlIntervalHours = crawlIntervalHours > 0 ? crawlIntervalHours : 168;
        this.isActive = true;
    }

    public void update(String url, String name, String description, Integer crawlIntervalHours) {
        if (url != null) this.url = url;
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (crawlIntervalHours != null) this.crawlIntervalHours = crawlIntervalHours;
    }

    public void updateSeoScore(BigDecimal seoScore) {
        this.seoScore = seoScore;
    }

    public void updateLastCrawledAt(LocalDateTime lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
