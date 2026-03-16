package com.seodashboard.api.keyword.dto;

import com.seodashboard.common.domain.Keyword;
import com.seodashboard.common.domain.KeywordRanking;

import java.time.LocalDateTime;

public record KeywordListResponse(
        Long id,
        String keyword,
        String targetUrl,
        String searchEngine,
        String countryCode,
        boolean isActive,
        Integer currentRank,
        Integer rankChange,
        LocalDateTime lastCheckedAt,
        LocalDateTime createdAt
) {

    public static KeywordListResponse from(Keyword keyword, KeywordRanking latestRanking) {
        return new KeywordListResponse(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getTargetUrl(),
                keyword.getSearchEngine(),
                keyword.getCountryCode(),
                keyword.isActive(),
                latestRanking != null ? latestRanking.getRank() : null,
                latestRanking != null ? latestRanking.getRankChange() : null,
                latestRanking != null ? latestRanking.getRecordedAt() : null,
                keyword.getCreatedAt()
        );
    }
}
