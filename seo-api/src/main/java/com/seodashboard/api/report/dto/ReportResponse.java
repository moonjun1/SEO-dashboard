package com.seodashboard.api.report.dto;

import com.seodashboard.common.domain.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long siteId,
        String siteName,
        String type,
        String title,
        LocalDate periodStart,
        LocalDate periodEnd,
        String summary,
        String status,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getSite().getId(),
                report.getSite().getName(),
                report.getType(),
                report.getTitle(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getSummary(),
                report.getStatus(),
                report.getCompletedAt(),
                report.getCreatedAt()
        );
    }
}
