package com.seodashboard.api.dashboard.dto;

public record SiteStatsResponse(
        String period,
        KeywordRankingDistribution keywordRankingDistribution
) {

    public record KeywordRankingDistribution(
            long top3,
            long top10,
            long top30,
            long top100,
            long notRanked
    ) {
    }
}
