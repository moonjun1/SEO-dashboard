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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String summary;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public Report(Site site, User user, String type, String title,
                  LocalDate periodStart, LocalDate periodEnd) {
        this.site = site;
        this.user = user;
        this.type = type;
        this.title = title;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = "PENDING";
    }

    public void markGenerating() {
        this.status = "GENERATING";
    }

    public void markCompleted(String summary) {
        this.status = "COMPLETED";
        this.summary = summary;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
    }
}
