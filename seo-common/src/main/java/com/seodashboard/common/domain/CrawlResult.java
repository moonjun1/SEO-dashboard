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

@Entity
@Table(name = "crawl_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_job_id", nullable = false)
    private CrawlJob crawlJob;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "content_length")
    private Long contentLength;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(length = 1000)
    private String title;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(nullable = false)
    private int depth;

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal;

    @Column(name = "redirect_url", length = 2048)
    private String redirectUrl;

    @Builder
    public CrawlResult(CrawlJob crawlJob, String url, Integer statusCode, String contentType,
                        Long contentLength, Integer responseTimeMs, String title,
                        String metaDescription, String canonicalUrl, int depth,
                        boolean isInternal, String redirectUrl) {
        this.crawlJob = crawlJob;
        this.url = url;
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.responseTimeMs = responseTimeMs;
        this.title = title;
        this.metaDescription = metaDescription;
        this.canonicalUrl = canonicalUrl;
        this.depth = depth;
        this.isInternal = isInternal;
        this.redirectUrl = redirectUrl;
    }
}
