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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private static final int BATCH_SIZE = 10;

    private final CrawlEngine crawlEngine;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final CrawlBatchPersister batchPersister;

    private final Map<Long, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    /**
     * Executes crawl without wrapping the entire operation in one transaction.
     * Individual batches are persisted in separate transactions via CrawlBatchPersister.
     */
    public void executeCrawl(Long crawlJobId) {
        CrawlJob crawlJob = batchPersister.findCrawlJob(crawlJobId);
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
            batchPersister.markRunning(crawlJobId);

            CrawlEngine.CrawlEngineResult engineResult = crawlEngine.crawl(
                    site.getUrl(), crawlJob.getMaxPages(), crawlJob.getMaxDepth(), cancelFlag);

            // Save results in batches to avoid long-running transactions
            List<CrawlPageResult> pages = engineResult.pages();
            BigDecimal totalScore = BigDecimal.ZERO;
            int pageCount = 0;

            for (int i = 0; i < pages.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, pages.size());
                List<CrawlPageResult> batch = pages.subList(i, end);
                batchPersister.saveBatch(crawlJobId, batch);
            }

            for (CrawlPageResult pageResult : pages) {
                if (pageResult.getSeoScore() != null) {
                    totalScore = totalScore.add(pageResult.getSeoScore());
                    pageCount++;
                }
            }

            if (cancelFlag.get()) {
                batchPersister.markCancelled(crawlJobId);
            } else {
                batchPersister.markCompleted(crawlJobId, pages.size(), engineResult.errorCount());
            }

            if (pageCount > 0) {
                BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(pageCount), 2, RoundingMode.HALF_UP);
                batchPersister.updateSiteScore(site.getId(), avgScore);
            }

            log.info("Crawl job completed: id={}, pages={}, errors={}",
                    crawlJobId, pages.size(), engineResult.errorCount());

        } catch (Exception e) {
            log.error("Crawl job failed: id={}, error={}", crawlJobId, e.getMessage(), e);
            batchPersister.markFailed(crawlJobId, e.getMessage());
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
}
