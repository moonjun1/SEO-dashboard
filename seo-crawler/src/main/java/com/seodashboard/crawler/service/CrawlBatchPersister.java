package com.seodashboard.crawler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.common.domain.CrawlJob;
import com.seodashboard.common.domain.CrawlResult;
import com.seodashboard.common.domain.PageAnalysis;
import com.seodashboard.common.domain.Site;
import com.seodashboard.crawler.dto.CrawlPageResult;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles persistence operations for crawl results in separate, short-lived transactions.
 * This prevents holding a DB connection for the entire crawl duration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlBatchPersister {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CrawlJob findCrawlJob(Long crawlJobId) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob != null && crawlJob.getSite() != null) {
            // Force-initialize the lazy Site proxy within this transaction
            crawlJob.getSite().getUrl();
        }
        return crawlJob;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRunning(Long crawlJobId) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob != null) {
            crawlJob.markRunning();
            entityManager.merge(crawlJob);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(Long crawlJobId, List<CrawlPageResult> batch) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob == null) return;

        for (CrawlPageResult pageResult : batch) {
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

        entityManager.flush();
        entityManager.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(Long crawlJobId, int totalPages, int errorCount) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob != null) {
            crawlJob.markCompleted(totalPages, errorCount);
            entityManager.merge(crawlJob);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelled(Long crawlJobId) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob != null) {
            crawlJob.markCancelled();
            entityManager.merge(crawlJob);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long crawlJobId, String errorMessage) {
        CrawlJob crawlJob = entityManager.find(CrawlJob.class, crawlJobId);
        if (crawlJob != null) {
            crawlJob.markFailed(errorMessage);
            entityManager.merge(crawlJob);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSiteScore(Long siteId, BigDecimal avgScore) {
        Site site = entityManager.find(Site.class, siteId);
        if (site != null) {
            site.updateSeoScore(avgScore);
            site.updateLastCrawledAt(LocalDateTime.now());
            entityManager.merge(site);
        }
    }
}
