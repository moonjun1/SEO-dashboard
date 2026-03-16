package com.seodashboard.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class RankingSimulator {

    /**
     * Simulate a keyword rank for demo purposes.
     * First collection: random rank between 5 and 50.
     * Subsequent collections: previous rank +/- 5 (clamped to 1~100).
     *
     * @param keyword       the keyword text
     * @param previousRank  the previous rank, or null if first collection
     * @return simulated rank (1~100), or null if rank > 100 (not in top 100)
     */
    public Integer simulateRank(String keyword, Integer previousRank) {
        int rank;

        if (previousRank == null) {
            rank = ThreadLocalRandom.current().nextInt(5, 51);
        } else {
            int delta = ThreadLocalRandom.current().nextInt(-5, 6);
            rank = previousRank + delta;
        }

        rank = Math.max(1, Math.min(100, rank));

        // 5% chance of dropping out of top 100
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            log.debug("Keyword '{}' dropped out of top 100", keyword);
            return null;
        }

        return rank;
    }

    /**
     * Simulate a URL that appeared in SERP for the keyword.
     */
    public String simulateUrl(String siteUrl, String keyword) {
        String base = siteUrl.endsWith("/") ? siteUrl : siteUrl + "/";
        String slug = keyword.toLowerCase().replaceAll("[^a-z0-9가-힣]+", "-").replaceAll("^-|-$", "");
        return base + slug;
    }

    /**
     * Simulate search volume for a keyword.
     */
    public Integer simulateSearchVolume(String keyword) {
        int baseVolume = keyword.length() * 100;
        int variation = ThreadLocalRandom.current().nextInt(-50, 51);
        return Math.max(10, baseVolume + variation);
    }
}
