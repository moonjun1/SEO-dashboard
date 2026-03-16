package com.seodashboard.api.keyword.service;

import com.seodashboard.api.keyword.dto.KeywordBatchCreateRequest;
import com.seodashboard.api.keyword.dto.KeywordCreateRequest;
import com.seodashboard.api.keyword.dto.KeywordListResponse;
import com.seodashboard.api.keyword.dto.KeywordResponse;
import com.seodashboard.api.keyword.dto.KeywordUpdateRequest;
import com.seodashboard.api.keyword.dto.RankingHistoryResponse;
import com.seodashboard.api.keyword.repository.KeywordRankingRepository;
import com.seodashboard.api.keyword.repository.KeywordRepository;
import com.seodashboard.api.site.repository.SiteRepository;
import com.seodashboard.common.domain.Keyword;
import com.seodashboard.common.domain.KeywordRanking;
import com.seodashboard.common.domain.Site;
import com.seodashboard.common.dto.PageResponse;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import com.seodashboard.scheduler.service.KeywordRankingCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final KeywordRankingRepository keywordRankingRepository;
    private final SiteRepository siteRepository;
    private final KeywordRankingCollector keywordRankingCollector;

    @Transactional
    public KeywordResponse createKeyword(Long siteId, Long userId, KeywordCreateRequest request) {
        Site site = findSiteByIdAndUserId(siteId, userId);

        String searchEngine = request.searchEngineOrDefault();
        String countryCode = request.countryCodeOrDefault();

        if (keywordRepository.existsBySiteIdAndKeywordAndSearchEngineAndCountryCode(
                siteId, request.keyword(), searchEngine, countryCode)) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEYWORD,
                    "Keyword '" + request.keyword() + "' already exists for this site");
        }

        Keyword keyword = Keyword.builder()
                .site(site)
                .keyword(request.keyword())
                .targetUrl(request.targetUrl())
                .searchEngine(searchEngine)
                .countryCode(countryCode)
                .languageCode(request.languageCodeOrDefault())
                .build();

        keyword = keywordRepository.save(keyword);
        log.info("Keyword created: id={}, keyword='{}', siteId={}", keyword.getId(), keyword.getKeyword(), siteId);

        return KeywordResponse.from(keyword, null);
    }

    @Transactional
    public List<KeywordResponse> batchCreateKeywords(Long siteId, Long userId, KeywordBatchCreateRequest request) {
        Site site = findSiteByIdAndUserId(siteId, userId);

        String searchEngine = request.searchEngineOrDefault();
        String countryCode = request.countryCodeOrDefault();
        String languageCode = request.languageCodeOrDefault();

        List<KeywordResponse> responses = new ArrayList<>();
        int skipped = 0;

        for (KeywordBatchCreateRequest.KeywordItem item : request.keywords()) {
            if (keywordRepository.existsBySiteIdAndKeywordAndSearchEngineAndCountryCode(
                    siteId, item.keyword(), searchEngine, countryCode)) {
                skipped++;
                log.debug("Skipping duplicate keyword: '{}'", item.keyword());
                continue;
            }

            Keyword keyword = Keyword.builder()
                    .site(site)
                    .keyword(item.keyword())
                    .targetUrl(item.targetUrl())
                    .searchEngine(searchEngine)
                    .countryCode(countryCode)
                    .languageCode(languageCode)
                    .build();

            keyword = keywordRepository.save(keyword);
            responses.add(KeywordResponse.from(keyword, null));
        }

        log.info("Batch keyword creation: siteId={}, created={}, skipped={}",
                siteId, responses.size(), skipped);

        return responses;
    }

    @Transactional(readOnly = true)
    public PageResponse<KeywordListResponse> getKeywords(Long siteId, Long userId,
                                                          Boolean isActive, String search,
                                                          Pageable pageable) {
        findSiteByIdAndUserId(siteId, userId);

        Page<Keyword> keywordPage;
        if (search != null && !search.isBlank()) {
            keywordPage = keywordRepository.findBySiteIdAndKeywordContaining(siteId, search, pageable);
        } else if (isActive != null) {
            keywordPage = keywordRepository.findBySiteIdAndIsActive(siteId, isActive, pageable);
        } else {
            keywordPage = keywordRepository.findBySiteId(siteId, pageable);
        }

        Page<KeywordListResponse> responsePage = keywordPage.map(keyword -> {
            KeywordRanking latestRanking = keywordRankingRepository
                    .findTopByKeywordIdOrderByRecordedAtDesc(keyword.getId())
                    .orElse(null);
            return KeywordListResponse.from(keyword, latestRanking);
        });

        return PageResponse.from(responsePage);
    }

    @Transactional
    public KeywordResponse updateKeyword(Long siteId, Long userId, Long keywordId, KeywordUpdateRequest request) {
        findSiteByIdAndUserId(siteId, userId);

        Keyword keyword = keywordRepository.findByIdAndSiteId(keywordId, siteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

        keyword.update(request.targetUrl(), request.isActive());
        log.info("Keyword updated: id={}, siteId={}", keywordId, siteId);

        KeywordRanking latestRanking = keywordRankingRepository
                .findTopByKeywordIdOrderByRecordedAtDesc(keywordId)
                .orElse(null);

        return KeywordResponse.from(keyword, latestRanking);
    }

    @Transactional
    public void deleteKeyword(Long siteId, Long userId, Long keywordId) {
        findSiteByIdAndUserId(siteId, userId);

        Keyword keyword = keywordRepository.findByIdAndSiteId(keywordId, siteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

        keywordRepository.delete(keyword);
        log.info("Keyword deleted: id={}, siteId={}", keywordId, siteId);
    }

    @Transactional(readOnly = true)
    public RankingHistoryResponse getRankingHistory(Long siteId, Long userId,
                                                     Long keywordId, String period) {
        findSiteByIdAndUserId(siteId, userId);

        Keyword keyword = keywordRepository.findByIdAndSiteId(keywordId, siteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

        if (period == null || period.isBlank()) {
            period = "30d";
        }

        LocalDateTime after = calculateAfterDate(period);
        List<KeywordRanking> rankings = keywordRankingRepository
                .findByKeywordIdAndRecordedAtAfterOrderByRecordedAtDesc(keywordId, after);

        List<RankingHistoryResponse.RankingEntry> entries = rankings.stream()
                .map(r -> new RankingHistoryResponse.RankingEntry(
                        r.getRecordedAt(),
                        r.getRank(),
                        r.getPreviousRank(),
                        r.getRankChange(),
                        r.getUrl()
                ))
                .toList();

        RankingHistoryResponse.RankingSummary summary = buildSummary(rankings);

        return new RankingHistoryResponse(
                keyword.getId(),
                keyword.getKeyword(),
                period,
                summary,
                entries
        );
    }

    @Transactional
    public int collectRankings(Long siteId, Long userId) {
        Site site = findSiteByIdAndUserId(siteId, userId);

        List<Keyword> activeKeywords = keywordRepository.findBySiteIdAndIsActiveTrue(siteId);
        if (activeKeywords.isEmpty()) {
            log.info("No active keywords found for siteId={}", siteId);
            return 0;
        }

        int collected = 0;
        for (Keyword keyword : activeKeywords) {
            Integer previousRank = keywordRankingRepository
                    .findTopByKeywordIdOrderByRecordedAtDesc(keyword.getId())
                    .map(KeywordRanking::getRank)
                    .orElse(null);

            KeywordRanking ranking = keywordRankingCollector.collectRanking(
                    keyword, site.getUrl(), previousRank);

            keywordRankingRepository.save(ranking);
            collected++;
        }

        log.info("Rankings collected: siteId={}, keywords={}", siteId, collected);
        return collected;
    }

    private Site findSiteByIdAndUserId(Long siteId, Long userId) {
        return siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));
    }

    private LocalDateTime calculateAfterDate(String period) {
        return switch (period) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "90d" -> LocalDateTime.now().minusDays(90);
            default -> LocalDateTime.now().minusDays(30);
        };
    }

    private RankingHistoryResponse.RankingSummary buildSummary(List<KeywordRanking> rankings) {
        if (rankings.isEmpty()) {
            return new RankingHistoryResponse.RankingSummary(null, null, null, null, null, "STABLE");
        }

        // Filter out null ranks (dropped out of top 100)
        List<Integer> ranks = rankings.stream()
                .map(KeywordRanking::getRank)
                .filter(r -> r != null)
                .toList();

        Integer currentRank = rankings.getFirst().getRank();
        OptionalInt bestRank = ranks.stream().mapToInt(Integer::intValue).min();
        OptionalInt worstRank = ranks.stream().mapToInt(Integer::intValue).max();
        OptionalDouble avgRank = ranks.stream().mapToInt(Integer::intValue).average();

        // Calculate rank change: first recorded rank vs current
        Integer firstRank = rankings.getLast().getRank();
        Integer rankChange = null;
        if (currentRank != null && firstRank != null) {
            rankChange = firstRank - currentRank; // positive = improved
        }

        // Trend: compare recent 7-day average vs previous 7-day average
        String trend = calculateTrend(rankings);

        return new RankingHistoryResponse.RankingSummary(
                currentRank,
                bestRank.isPresent() ? bestRank.getAsInt() : null,
                worstRank.isPresent() ? worstRank.getAsInt() : null,
                avgRank.isPresent() ? Math.round(avgRank.getAsDouble() * 100.0) / 100.0 : null,
                rankChange,
                trend
        );
    }

    private String calculateTrend(List<KeywordRanking> rankings) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime fourteenDaysAgo = now.minusDays(14);

        OptionalDouble recentAvg = rankings.stream()
                .filter(r -> r.getRecordedAt().isAfter(sevenDaysAgo) && r.getRank() != null)
                .mapToInt(KeywordRanking::getRank)
                .average();

        OptionalDouble previousAvg = rankings.stream()
                .filter(r -> r.getRecordedAt().isAfter(fourteenDaysAgo)
                        && r.getRecordedAt().isBefore(sevenDaysAgo)
                        && r.getRank() != null)
                .mapToInt(KeywordRanking::getRank)
                .average();

        if (recentAvg.isEmpty() || previousAvg.isEmpty()) {
            return "STABLE";
        }

        double diff = previousAvg.getAsDouble() - recentAvg.getAsDouble();
        if (diff > 2) {
            return "IMPROVING";
        } else if (diff < -2) {
            return "DECLINING";
        }
        return "STABLE";
    }
}
