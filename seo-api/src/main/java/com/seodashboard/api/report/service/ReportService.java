package com.seodashboard.api.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seodashboard.api.crawl.repository.CrawlJobRepository;
import com.seodashboard.api.crawl.repository.CrawlResultRepository;
import com.seodashboard.api.crawl.repository.PageAnalysisRepository;
import com.seodashboard.api.keyword.repository.KeywordRankingRepository;
import com.seodashboard.api.keyword.repository.KeywordRepository;
import com.seodashboard.api.report.dto.ReportCreateRequest;
import com.seodashboard.api.report.dto.ReportListResponse;
import com.seodashboard.api.report.dto.ReportResponse;
import com.seodashboard.api.report.repository.ReportRepository;
import com.seodashboard.api.site.repository.SiteRepository;
import com.seodashboard.common.domain.Keyword;
import com.seodashboard.common.domain.KeywordRanking;
import com.seodashboard.common.domain.Report;
import com.seodashboard.common.domain.Site;
import com.seodashboard.common.dto.PageResponse;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final SiteRepository siteRepository;
    private final KeywordRepository keywordRepository;
    private final KeywordRankingRepository keywordRankingRepository;
    private final CrawlJobRepository crawlJobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReportResponse generateReport(Long siteId, Long userId, ReportCreateRequest request) {
        Site site = siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));

        Report report = Report.builder()
                .site(site)
                .user(site.getUser())
                .type(request.type())
                .title(request.titleOrDefault())
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .build();

        report.markGenerating();
        report = reportRepository.save(report);

        try {
            String summary = buildSummaryJson(site);
            report.markCompleted(summary);
            log.info("Report generated: id={}, siteId={}", report.getId(), siteId);
        } catch (Exception e) {
            report.markFailed();
            log.error("Report generation failed: siteId={}", siteId, e);
        }

        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportListResponse> getReports(Long siteId, Long userId, Pageable pageable) {
        siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));

        Page<Report> page = reportRepository.findBySiteIdOrderByCreatedAtDesc(siteId, pageable);
        return PageResponse.from(page.map(ReportListResponse::from));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long siteId, Long userId, Long reportId) {
        siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));

        Report report = reportRepository.findByIdAndSiteId(reportId, siteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        return ReportResponse.from(report);
    }

    private String buildSummaryJson(Site site) {
        try {
            ObjectNode summary = objectMapper.createObjectNode();

            // Overall score
            BigDecimal seoScore = site.getSeoScore();
            summary.put("overallScore", seoScore != null ? seoScore.doubleValue() : 0.0);

            // Pages crawled from latest crawl job
            Page<com.seodashboard.common.domain.CrawlJob> latestJobs =
                    crawlJobRepository.findBySiteIdOrderByCreatedAtDesc(site.getId(), PageRequest.of(0, 1));
            int pagesCrawled = 0;
            if (!latestJobs.isEmpty()) {
                Integer totalPages = latestJobs.getContent().getFirst().getTotalPages();
                pagesCrawled = totalPages != null ? totalPages : 0;
            }
            summary.put("pagesCrawled", pagesCrawled);

            // Issues count - simplified: use broken_links_count from page analyses
            summary.put("issuesFound", 0);

            // Top keywords
            ArrayNode topKeywordsArray = summary.putArray("topKeywords");
            List<Keyword> activeKeywords = keywordRepository.findBySiteIdAndIsActiveTrue(site.getId());
            int keywordLimit = Math.min(activeKeywords.size(), 5);
            for (int i = 0; i < keywordLimit; i++) {
                Keyword kw = activeKeywords.get(i);
                KeywordRanking latestRanking = keywordRankingRepository
                        .findTopByKeywordIdOrderByRecordedAtDesc(kw.getId())
                        .orElse(null);

                ObjectNode kwNode = objectMapper.createObjectNode();
                kwNode.put("keyword", kw.getKeyword());
                kwNode.put("rank", latestRanking != null && latestRanking.getRank() != null
                        ? latestRanking.getRank() : -1);
                kwNode.put("rankChange", latestRanking != null && latestRanking.getRankChange() != null
                        ? latestRanking.getRankChange() : 0);
                topKeywordsArray.add(kwNode);
            }

            // Priority actions
            ArrayNode priorityActions = summary.putArray("priorityActions");
            if (seoScore != null && seoScore.compareTo(BigDecimal.valueOf(70)) < 0) {
                priorityActions.add("Improve overall SEO score (currently " + seoScore + ")");
            }
            if (pagesCrawled == 0) {
                priorityActions.add("Run initial site crawl to identify SEO issues");
            }
            if (activeKeywords.isEmpty()) {
                priorityActions.add("Add target keywords for ranking tracking");
            }

            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            log.error("Failed to build summary JSON for site: {}", site.getId(), e);
            return "{}";
        }
    }
}
