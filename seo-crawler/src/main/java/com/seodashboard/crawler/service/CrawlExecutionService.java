package com.seodashboard.crawler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.common.domain.CrawlJob;
import com.seodashboard.common.domain.CrawlResult;
import com.seodashboard.common.domain.PageAnalysis;
import com.seodashboard.common.domain.Site;
import com.seodashboard.crawler.dto.CrawlPageResult;
import com.seodashboard.crawler.engine.CrawlEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private final CrawlEngine crawlEngine;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    private final Map<Long, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    @Transactional
    public void executeCrawl(Long crawlJobId) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob == null) {
            log.error("CrawlJob not found: {}", crawlJobId);
            return;
        }

        Site site = crawlJob.getSite();
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        cancelFlags.put(crawlJobId, cancelFlag);

        log.info("Starting crawl job: id={}, siteUrl={}, maxPages={}, maxDepth={}",
                crawlJobId, site.getUrl(), crawlJob.getMaxPages(), crawlJob.getMaxDepth());

        try {
            crawlJob.markRunning();
            entityManager.merge(crawlJob);
            entityManager.flush();

            CrawlEngine.CrawlEngineResult engineResult = crawlEngine.crawl(
                    site.getUrl(), crawlJob.getMaxPages(), crawlJob.getMaxDepth(), cancelFlag);

            // Save each page result
            BigDecimal totalScore = BigDecimal.ZERO;
            int pageCount = 0;

            for (CrawlPageResult pageResult : engineResult.pages()) {
                CrawlResult crawlResult = saveCrawlResult(crawlJob, pageResult);
                savePageAnalysis(crawlResult, pageResult);

                if (pageResult.getSeoScore() != null) {
                    totalScore = totalScore.add(pageResult.getSeoScore());
                    pageCount++;
                }
            }

            // Update crawl job status
            if (cancelFlag.get()) {
                crawlJob.markCancelled();
            } else {
                crawlJob.markCompleted(engineResult.pages().size(), engineResult.errorCount());
            }
            entityManager.merge(crawlJob);

            // Update site SEO score and last crawled time
            if (pageCount > 0) {
                BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(pageCount), 2, RoundingMode.HALF_UP);
                site.updateSeoScore(avgScore);
            }
            site.updateLastCrawledAt(LocalDateTime.now());
            entityManager.merge(site);

            log.info("Crawl job completed: id={}, pages={}, errors={}",
                    crawlJobId, engineResult.pages().size(), engineResult.errorCount());

        } catch (Exception e) {
            log.error("Crawl job failed: id={}, error={}", crawlJobId, e.getMessage(), e);
            crawlJob.markFailed(e.getMessage());
            entityManager.merge(crawlJob);
        } finally {
            cancelFlags.remove(crawlJobId);
        }
    }

    public void cancelCrawl(Long crawlJobId) {
        AtomicBoolean flag = cancelFlags.get(crawlJobId);
        if (flag != null) {
            flag.set(true);
            log.info("Cancel flag set for crawl job: {}", crawlJobId);
        }
    }

    private CrawlResult saveCrawlResult(CrawlJob crawlJob, CrawlPageResult pageResult) {
        CrawlResult crawlResult = CrawlResult.builder()
                .crawlJob(crawlJob)
                .url(pageResult.getUrl())
                .statusCode(pageResult.getStatusCode())
                .contentType(pageResult.getContentType())
                .contentLength(pageResult.getContentLength())
                .responseTimeMs(pageResult.getResponseTimeMs())
                .title(pageResult.getTitle())
                .metaDescription(pageResult.getMetaDescription())
                .canonicalUrl(pageResult.getCanonicalUrl())
                .depth(pageResult.getDepth())
                .isInternal(pageResult.isInternal())
                .redirectUrl(pageResult.getRedirectUrl())
                .build();

        entityManager.persist(crawlResult);
        return crawlResult;
    }

    private void savePageAnalysis(CrawlResult crawlResult, CrawlPageResult pageResult) {
        String issuesJson;
        try {
            issuesJson = objectMapper.writeValueAsString(pageResult.getIssues());
        } catch (JsonProcessingException e) {
            issuesJson = "[]";
        }

        PageAnalysis pageAnalysis = PageAnalysis.builder()
                .crawlResult(crawlResult)
                .seoScore(pageResult.getSeoScore())
                .titleScore(pageResult.getTitleScore())
                .titleLength(pageResult.getTitleLength())
                .metaDescriptionScore(pageResult.getMetaDescriptionScore())
                .metaDescriptionLength(pageResult.getMetaDescriptionLength())
                .headingScore(pageResult.getHeadingScore())
                .headingStructure(pageResult.getHeadingStructure())
                .imageScore(pageResult.getImageScore())
                .imagesTotal(pageResult.getImagesTotal())
                .imagesWithoutAlt(pageResult.getImagesWithoutAlt())
                .linkScore(pageResult.getLinkScore())
                .internalLinksCount(pageResult.getInternalLinksCount())
                .externalLinksCount(pageResult.getExternalLinksCount())
                .brokenLinksCount(pageResult.getBrokenLinksCount())
                .performanceScore(pageResult.getPerformanceScore())
                .hasOgTags(pageResult.getHasOgTags())
                .hasTwitterCards(pageResult.getHasTwitterCards())
                .hasStructuredData(pageResult.getHasStructuredData())
                .hasSitemap(pageResult.getHasSitemap())
                .hasRobotsTxt(pageResult.getHasRobotsTxt())
                .isMobileFriendly(pageResult.getIsMobileFriendly())
                .issues(issuesJson)
                .build();

        entityManager.persist(pageAnalysis);
    }
}
