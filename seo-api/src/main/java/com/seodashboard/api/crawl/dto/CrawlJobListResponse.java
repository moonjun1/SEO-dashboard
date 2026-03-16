package com.seodashboard.api.crawl.dto;

import com.seodashboard.common.domain.CrawlJob;

import java.time.LocalDateTime;

public record CrawlJobListResponse(
        Long id,
        String status,
        String triggerType,
        int maxPages,
        Integer totalPages,
        int errorCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static CrawlJobListResponse from(CrawlJob crawlJob) {
        return new CrawlJobListResponse(
                crawlJob.getId(),
                crawlJob.getStatus(),
                crawlJob.getTriggerType(),
                crawlJob.getMaxPages(),
                crawlJob.getTotalPages(),
                crawlJob.getErrorCount(),
                crawlJob.getStartedAt(),
                crawlJob.getCompletedAt(),
                crawlJob.getCreatedAt()
        );
    }
}
