package com.seodashboard.api.dashboard.service;

import com.seodashboard.api.crawl.repository.CrawlJobRepository;
import com.seodashboard.api.crawl.repository.CrawlResultRepository;
import com.seodashboard.api.crawl.repository.PageAnalysisRepository;
import com.seodashboard.api.dashboard.dto.DashboardResponse;
import com.seodashboard.api.dashboard.dto.SiteDashboardResponse;
import com.seodashboard.api.dashboard.dto.SiteStatsResponse;
import com.seodashboard.api.keyword.repository.KeywordRankingRepository;
import com.seodashboard.api.keyword.repository.KeywordRepository;
import com.seodashboard.api.notification.repository.NotificationRepository;
import com.seodashboard.api.site.repository.SiteRepository;
import com.seodashboard.common.domain.CrawlJob;
import com.seodashboard.common.domain.CrawlResult;
import com.seodashboard.common.domain.Keyword;
import com.seodashboard.common.domain.KeywordRanking;
import com.seodashboard.common.domain.Notification;
import com.seodashboard.common.domain.PageAnalysis;
import com.seodashboard.common.domain.Site;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SiteRepository siteRepository;
    private final KeywordRepository keywordRepository;
    private final KeywordRankingRepository keywordRankingRepository;
    private final CrawlJobRepository crawlJobRepository;
    private final CrawlResultRepository crawlResultRepository;
    private final PageAnalysisRepository pageAnalysisRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        // Get all active sites for user
        Page<Site> sitesPage = siteRepository.findByUserIdAndIsActiveTrue(userId,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Site> sites = sitesPage.getContent();

        int totalSites = sites.size();
        long totalKeywords = 0;
        long keywordsInTop10 = 0;
        long totalIssues = 0;
        long criticalIssues = 0;
        BigDecimal seoScoreSum = BigDecimal.ZERO;
        int seoScoreCount = 0;

        List<DashboardResponse.SiteSummary> siteSummaries = new ArrayList<>();

        // Batch-load all keywords for all sites at once
        List<Long> siteIds = sites.stream().map(Site::getId).toList();
        Map<Long, List<Keyword>> keywordsBySiteId = siteIds.isEmpty()
                ? Map.of()
                : keywordRepository.findBySiteIdInAndIsActiveTrue(siteIds).stream()
                    .collect(Collectors.groupingBy(kw -> kw.getSite().getId()));

        // Batch-load latest rankings for all keywords in one query
        List<Long> allKeywordIds = keywordsBySiteId.values().stream()
                .flatMap(List::stream)
                .map(Keyword::getId)
                .toList();
        Map<Long, KeywordRanking> latestRankingByKeywordId = allKeywordIds.isEmpty()
                ? Map.of()
                : keywordRankingRepository.findLatestByKeywordIdIn(allKeywordIds).stream()
                    .collect(Collectors.toMap(kr -> kr.getKeyword().getId(), Function.identity()));

        for (Site site : sites) {
            List<Keyword> siteKeywords = keywordsBySiteId.getOrDefault(site.getId(), List.of());
            long siteKeywordCount = siteKeywords.size();
            totalKeywords += siteKeywordCount;

            // Count keywords in top 10 using pre-loaded rankings
            long siteTop10 = 0;
            for (Keyword kw : siteKeywords) {
                KeywordRanking latest = latestRankingByKeywordId.get(kw.getId());
                if (latest != null && latest.getRank() != null && latest.getRank() <= 10) {
                    siteTop10++;
                }
            }
            keywordsInTop10 += siteTop10;

            // SEO score
            if (site.getSeoScore() != null) {
                seoScoreSum = seoScoreSum.add(site.getSeoScore());
                seoScoreCount++;
            }

            // Issue count from latest crawl - simplified
            long siteIssueCount = 0;

            siteSummaries.add(new DashboardResponse.SiteSummary(
                    site.getId(),
                    site.getName(),
                    site.getUrl(),
                    site.getSeoScore(),
                    siteKeywordCount,
                    siteIssueCount,
                    site.getLastCrawledAt()
            ));
        }

        BigDecimal avgSeoScore = seoScoreCount > 0
                ? seoScoreSum.divide(BigDecimal.valueOf(seoScoreCount), 2, RoundingMode.HALF_UP)
                : null;

        // Recent activity - latest 5 notifications
        Page<Notification> recentNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5));
        List<DashboardResponse.RecentActivity> recentActivity = recentNotifications.getContent().stream()
                .map(n -> new DashboardResponse.RecentActivity(
                        n.getId(),
                        n.getType(),
                        n.getTitle(),
                        n.getSeverity(),
                        n.getCreatedAt()
                ))
                .toList();

        return new DashboardResponse(
                totalSites,
                avgSeoScore,
                totalKeywords,
                keywordsInTop10,
                totalIssues,
                criticalIssues,
                siteSummaries,
                recentActivity
        );
    }

    @Transactional(readOnly = true)
    public SiteDashboardResponse getSiteDashboard(Long siteId, Long userId) {
        Site site = siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));

        // Overview
        int totalPages = 0;
        int healthyPages = 0;
        int issuePages = 0;
        Double avgResponseTimeMs = null;

        Page<CrawlJob> latestJobs = crawlJobRepository.findBySiteIdOrderByCreatedAtDesc(
                siteId, PageRequest.of(0, 1));

        if (!latestJobs.isEmpty()) {
            CrawlJob latestJob = latestJobs.getContent().getFirst();
            if (latestJob.getTotalPages() != null) {
                totalPages = latestJob.getTotalPages();
            }

            // Get crawl results for the latest job to compute avg response time and issue pages
            Page<CrawlResult> results = crawlResultRepository.findByCrawlJobIdOrderBySeoScoreAsc(
                    latestJob.getId(), PageRequest.of(0, 1000));

            // Batch-load all page analyses for these crawl results
            List<Long> crawlResultIds = results.getContent().stream()
                    .map(CrawlResult::getId).toList();
            Map<Long, PageAnalysis> analysisByCrawlResultId = crawlResultIds.isEmpty()
                    ? Map.of()
                    : pageAnalysisRepository.findByCrawlResultIdIn(crawlResultIds).stream()
                        .collect(Collectors.toMap(pa -> pa.getCrawlResult().getId(), Function.identity()));

            long responseTimeSum = 0;
            int responseTimeCount = 0;

            for (CrawlResult cr : results.getContent()) {
                if (cr.getResponseTimeMs() != null) {
                    responseTimeSum += cr.getResponseTimeMs();
                    responseTimeCount++;
                }

                PageAnalysis pa = analysisByCrawlResultId.get(cr.getId());
                if (pa != null) {
                    if (pa.getSeoScore() != null && pa.getSeoScore().compareTo(BigDecimal.valueOf(70)) >= 0) {
                        healthyPages++;
                    } else {
                        issuePages++;
                    }
                }
            }

            if (responseTimeCount > 0) {
                avgResponseTimeMs = (double) responseTimeSum / responseTimeCount;
            }
        }

        SiteDashboardResponse.Overview overview = new SiteDashboardResponse.Overview(
                site.getSeoScore(), totalPages, healthyPages, issuePages, avgResponseTimeMs);

        // Issues summary - simplified counts
        long criticalCount = 0;
        long warningCount = 0;
        long infoCount = 0;
        SiteDashboardResponse.IssuesSummary issuesSummary = new SiteDashboardResponse.IssuesSummary(
                criticalCount, warningCount, infoCount, criticalCount + warningCount + infoCount);

        // Top keywords
        List<Keyword> activeKeywords = keywordRepository.findBySiteIdAndIsActiveTrue(siteId);

        // Batch-load latest rankings for all active keywords
        List<Long> activeKeywordIds = activeKeywords.stream().map(Keyword::getId).toList();
        Map<Long, KeywordRanking> siteLatestRankings = activeKeywordIds.isEmpty()
                ? Map.of()
                : keywordRankingRepository.findLatestByKeywordIdIn(activeKeywordIds).stream()
                    .collect(Collectors.toMap(kr -> kr.getKeyword().getId(), Function.identity()));

        List<SiteDashboardResponse.TopKeyword> topKeywords = new ArrayList<>();
        int keywordLimit = Math.min(activeKeywords.size(), 5);
        for (int i = 0; i < keywordLimit; i++) {
            Keyword kw = activeKeywords.get(i);
            KeywordRanking latest = siteLatestRankings.get(kw.getId());

            topKeywords.add(new SiteDashboardResponse.TopKeyword(
                    kw.getId(),
                    kw.getKeyword(),
                    latest != null ? latest.getRank() : null,
                    latest != null ? latest.getPreviousRank() : null,
                    latest != null ? latest.getRankChange() : null
            ));
        }

        // Improvement priority
        List<SiteDashboardResponse.ImprovementItem> improvements = buildImprovementPriority(
                site, totalPages, healthyPages, activeKeywords);

        return new SiteDashboardResponse(overview, issuesSummary, topKeywords, improvements);
    }

    @Transactional(readOnly = true)
    public SiteStatsResponse getSiteStats(Long siteId, Long userId, String period) {
        Site site = siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));

        if (period == null || period.isBlank()) {
            period = "30d";
        }

        LocalDateTime after = calculateAfterDate(period);

        List<Keyword> activeKeywords = keywordRepository.findBySiteIdAndIsActiveTrue(siteId);

        // Batch-load latest rankings for all active keywords
        List<Long> statsKeywordIds = activeKeywords.stream().map(Keyword::getId).toList();
        Map<Long, KeywordRanking> statsLatestRankings = statsKeywordIds.isEmpty()
                ? Map.of()
                : keywordRankingRepository.findLatestByKeywordIdIn(statsKeywordIds).stream()
                    .collect(Collectors.toMap(kr -> kr.getKeyword().getId(), Function.identity()));

        long top3 = 0;
        long top10 = 0;
        long top30 = 0;
        long top100 = 0;
        long notRanked = 0;

        for (Keyword kw : activeKeywords) {
            KeywordRanking latest = statsLatestRankings.get(kw.getId());

            if (latest == null || latest.getRank() == null) {
                notRanked++;
            } else {
                int rank = latest.getRank();
                if (rank <= 3) {
                    top3++;
                } else if (rank <= 10) {
                    top10++;
                } else if (rank <= 30) {
                    top30++;
                } else if (rank <= 100) {
                    top100++;
                } else {
                    notRanked++;
                }
            }
        }

        SiteStatsResponse.KeywordRankingDistribution distribution =
                new SiteStatsResponse.KeywordRankingDistribution(top3, top10, top30, top100, notRanked);

        return new SiteStatsResponse(period, distribution);
    }

    private LocalDateTime calculateAfterDate(String period) {
        return switch (period) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "90d" -> LocalDateTime.now().minusDays(90);
            default -> LocalDateTime.now().minusDays(30);
        };
    }

    private List<SiteDashboardResponse.ImprovementItem> buildImprovementPriority(
            Site site, int totalPages, int healthyPages, List<Keyword> activeKeywords) {

        List<SiteDashboardResponse.ImprovementItem> items = new ArrayList<>();

        if (site.getSeoScore() == null || site.getSeoScore().compareTo(BigDecimal.valueOf(50)) < 0) {
            items.add(new SiteDashboardResponse.ImprovementItem(
                    "SEO Score", "Overall SEO score is below 50. Run a full crawl and address critical issues.",
                    "CRITICAL", totalPages));
        } else if (site.getSeoScore().compareTo(BigDecimal.valueOf(70)) < 0) {
            items.add(new SiteDashboardResponse.ImprovementItem(
                    "SEO Score", "SEO score can be improved. Focus on meta descriptions and heading structure.",
                    "WARNING", totalPages));
        }

        int issuePages = totalPages - healthyPages;
        if (issuePages > 0) {
            items.add(new SiteDashboardResponse.ImprovementItem(
                    "Page Health", issuePages + " pages have SEO issues that need attention.",
                    "WARNING", issuePages));
        }

        if (activeKeywords.isEmpty()) {
            items.add(new SiteDashboardResponse.ImprovementItem(
                    "Keywords", "No active keywords. Add keywords to track search ranking performance.",
                    "INFO", 0));
        }

        if (site.getLastCrawledAt() == null) {
            items.add(new SiteDashboardResponse.ImprovementItem(
                    "Crawl", "Site has never been crawled. Run an initial crawl to get SEO insights.",
                    "CRITICAL", 0));
        } else if (site.getLastCrawledAt().isBefore(LocalDateTime.now().minusDays(30))) {
            items.add(new SiteDashboardResponse.ImprovementItem(
                    "Crawl", "Last crawl was more than 30 days ago. Consider re-crawling for fresh data.",
                    "WARNING", 0));
        }

        return items;
    }
}
