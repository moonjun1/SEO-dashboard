package com.seodashboard.api.report.dto;

import com.seodashboard.common.domain.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportListResponse(
        Long id,
        String type,
        String title,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static ReportListResponse from(Report report) {
        return new ReportListResponse(
                report.getId(),
                report.getType(),
                report.getTitle(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getStatus().name(),
                report.getCompletedAt(),
                report.getCreatedAt()
        );
    }
}
