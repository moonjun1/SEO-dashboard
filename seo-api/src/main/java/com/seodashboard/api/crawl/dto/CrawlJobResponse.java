package com.seodashboard.api.crawl.dto;

import com.seodashboard.common.domain.CrawlJob;

import java.time.LocalDateTime;

public record CrawlJobResponse(
        Long id,
        Long siteId,
        String status,
        String triggerType,
        int maxPages,
        int maxDepth,
        Integer totalPages,
        int errorCount,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static CrawlJobResponse from(CrawlJob crawlJob) {
        return new CrawlJobResponse(
                crawlJob.getId(),
                crawlJob.getSite().getId(),
                crawlJob.getStatus(),
                crawlJob.getTriggerType(),
                crawlJob.getMaxPages(),
                crawlJob.getMaxDepth(),
                crawlJob.getTotalPages(),
                crawlJob.getErrorCount(),
                crawlJob.getErrorMessage(),
                crawlJob.getStartedAt(),
                crawlJob.getCompletedAt(),
                crawlJob.getCreatedAt()
        );
    }
}
