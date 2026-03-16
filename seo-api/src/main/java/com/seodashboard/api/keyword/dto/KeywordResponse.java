package com.seodashboard.api.keyword.dto;

import com.seodashboard.common.domain.Keyword;
import com.seodashboard.common.domain.KeywordRanking;

import java.time.LocalDateTime;

public record KeywordResponse(
        Long id,
        Long siteId,
        String keyword,
        String targetUrl,
        String searchEngine,
        String countryCode,
        String languageCode,
        boolean isActive,
        LatestRanking latestRanking,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record LatestRanking(
            Integer rank,
            Integer previousRank,
            Integer rankChange,
            String url,
            Integer searchVolume,
            LocalDateTime recordedAt
    ) {
        public static LatestRanking from(KeywordRanking ranking) {
            if (ranking == null) return null;
            return new LatestRanking(
                    ranking.getRank(),
                    ranking.getPreviousRank(),
                    ranking.getRankChange(),
                    ranking.getUrl(),
                    ranking.getSearchVolume(),
                    ranking.getRecordedAt()
            );
        }
    }

    public static KeywordResponse from(Keyword keyword, KeywordRanking latestRanking) {
        return new KeywordResponse(
                keyword.getId(),
                keyword.getSite().getId(),
                keyword.getKeyword(),
                keyword.getTargetUrl(),
                keyword.getSearchEngine(),
                keyword.getCountryCode(),
                keyword.getLanguageCode(),
                keyword.isActive(),
                LatestRanking.from(latestRanking),
                keyword.getCreatedAt(),
                keyword.getUpdatedAt()
        );
    }
}
