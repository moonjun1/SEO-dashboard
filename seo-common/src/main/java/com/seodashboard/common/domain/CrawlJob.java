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

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "max_pages", nullable = false)
    private int maxPages;

    @Column(name = "max_depth", nullable = false)
    private int maxDepth;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public CrawlJob(Site site, String triggerType, int maxPages, int maxDepth) {
        this.site = site;
        this.status = "PENDING";
        this.triggerType = triggerType;
        this.maxPages = maxPages;
        this.maxDepth = maxDepth;
        this.errorCount = 0;
    }

    public void markRunning() {
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted(int totalPages, int errorCount) {
        this.status = "COMPLETED";
        this.totalPages = totalPages;
        this.errorCount = errorCount;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = "CANCELLED";
        this.completedAt = LocalDateTime.now();
    }

    public boolean isRunning() {
        return "RUNNING".equals(this.status);
    }

    public boolean isCancellable() {
        return "PENDING".equals(this.status) || "RUNNING".equals(this.status);
    }
}
