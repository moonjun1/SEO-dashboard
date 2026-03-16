package com.seodashboard.api.keyword.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RankingHistoryResponse(
        Long keywordId,
        String keyword,
        String period,
        RankingSummary summary,
        List<RankingEntry> rankings
) {

    public record RankingSummary(
            Integer currentRank,
            Integer bestRank,
            Integer worstRank,
            Double avgRank,
            Integer rankChange,
            String trend
    ) {}

    public record RankingEntry(
            LocalDateTime recordedAt,
            Integer rank,
            Integer previousRank,
            Integer rankChange,
            String url
    ) {}
}
