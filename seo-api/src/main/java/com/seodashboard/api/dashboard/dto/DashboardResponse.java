package com.seodashboard.api.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        int totalSites,
        BigDecimal avgSeoScore,
        long totalKeywords,
        long keywordsInTop10,
        long totalIssues,
        long criticalIssues,
        List<SiteSummary> sites,
        List<RecentActivity> recentActivity
) {

    public record SiteSummary(
            Long id,
            String name,
            String url,
            BigDecimal seoScore,
            long keywordCount,
            long issueCount,
            LocalDateTime lastCrawledAt
    ) {
    }

    public record RecentActivity(
            Long id,
            String type,
            String title,
            String severity,
            LocalDateTime createdAt
    ) {
    }
}
