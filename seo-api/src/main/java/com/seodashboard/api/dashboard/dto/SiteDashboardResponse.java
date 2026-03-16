package com.seodashboard.api.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record SiteDashboardResponse(
        Overview overview,
        IssuesSummary issuesSummary,
        List<TopKeyword> topKeywords,
        List<ImprovementItem> improvementPriority
) {

    public record Overview(
            BigDecimal seoScore,
            int totalPages,
            int healthyPages,
            int issuePages,
            Double avgResponseTimeMs
    ) {
    }

    public record IssuesSummary(
            long critical,
            long warning,
            long info,
            long total
    ) {
    }

    public record TopKeyword(
            Long keywordId,
            String keyword,
            Integer currentRank,
            Integer previousRank,
            Integer rankChange
    ) {
    }

    public record ImprovementItem(
            String category,
            String description,
            String severity,
            int affectedPages
    ) {
    }
}
