package com.seodashboard.scheduler.service;

import com.seodashboard.common.domain.Keyword;
import com.seodashboard.common.domain.KeywordRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordRankingCollector {

    private final RankingSimulator rankingSimulator;

    /**
     * Collect ranking for a single keyword.
     *
     * @param keyword      the keyword entity
     * @param siteUrl      the site URL (for URL simulation)
     * @param previousRank the most recent rank for this keyword, or null if first collection
     * @return a new KeywordRanking entity (not yet persisted)
     */
    public KeywordRanking collectRanking(Keyword keyword, String siteUrl, Integer previousRank) {
        Integer rank = rankingSimulator.simulateRank(keyword.getKeyword(), previousRank);
        String url = rank != null ? rankingSimulator.simulateUrl(siteUrl, keyword.getKeyword()) : null;
        Integer searchVolume = rankingSimulator.simulateSearchVolume(keyword.getKeyword());

        Integer rankChange = null;
        if (previousRank != null && rank != null) {
            // Positive = improved (went up in rank = lower number)
            rankChange = previousRank - rank;
        }

        log.debug("Collected ranking for keyword '{}': rank={}, previousRank={}, change={}",
                keyword.getKeyword(), rank, previousRank, rankChange);

        return KeywordRanking.builder()
                .keyword(keyword)
                .recordedAt(LocalDateTime.now())
                .rank(rank)
                .url(url)
                .searchVolume(searchVolume)
                .previousRank(previousRank)
                .rankChange(rankChange)
                .build();
    }
}
