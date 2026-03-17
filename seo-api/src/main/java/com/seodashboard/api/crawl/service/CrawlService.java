package com.seodashboard.api.crawl.service;

import com.seodashboard.api.crawl.dto.CrawlJobListResponse;
import com.seodashboard.api.crawl.dto.CrawlJobResponse;
import com.seodashboard.api.crawl.dto.CrawlResultResponse;
import com.seodashboard.api.crawl.dto.CrawlStartRequest;
import com.seodashboard.api.crawl.repository.CrawlJobRepository;
import com.seodashboard.api.crawl.repository.CrawlResultRepository;
import com.seodashboard.api.crawl.repository.PageAnalysisRepository;
import com.seodashboard.api.site.repository.SiteRepository;
import com.seodashboard.common.domain.CrawlJob;
import com.seodashboard.common.domain.CrawlResult;
import com.seodashboard.common.domain.PageAnalysis;
import com.seodashboard.common.domain.Site;
import com.seodashboard.common.dto.PageResponse;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.seodashboard.api.crawl.event.CrawlStartedEvent;
import com.seodashboard.crawler.service.CrawlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlJobRepository crawlJobRepository;
    private final CrawlResultRepository crawlResultRepository;
    private final PageAnalysisRepository pageAnalysisRepository;
    private final SiteRepository siteRepository;
    private final CrawlExecutionService crawlExecutionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CrawlJobResponse startCrawl(Long siteId, Long userId, CrawlStartRequest request) {
        Site site = siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));

        if (crawlJobRepository.existsBySiteIdAndStatus(siteId, "RUNNING")) {
            throw new BusinessException(ErrorCode.CRAWL_ALREADY_RUNNING);
        }

        CrawlJob crawlJob = CrawlJob.builder()
                .site(site)
                .triggerType("MANUAL")
                .maxPages(request.maxPagesOrDefault())
                .maxDepth(request.maxDepthOrDefault())
                .build();

        crawlJob = crawlJobRepository.save(crawlJob);
        log.info("Crawl job created: id={}, siteId={}, maxPages={}, maxDepth={}",
                crawlJob.getId(), siteId, crawlJob.getMaxPages(), crawlJob.getMaxDepth());

        // Publish event - will trigger async crawl after transaction commits
        eventPublisher.publishEvent(new CrawlStartedEvent(crawlJob.getId()));

        return CrawlJobResponse.from(crawlJob);
    }

    @Transactional(readOnly = true)
    public PageResponse<CrawlJobListResponse> getCrawlJobs(Long siteId, Long userId, Pageable pageable) {
        validateSiteAccess(siteId, userId);

        Page<CrawlJobListResponse> page = crawlJobRepository
                .findBySiteIdOrderByCreatedAtDesc(siteId, pageable)
                .map(CrawlJobListResponse::from);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public CrawlJobResponse getCrawlJob(Long siteId, Long userId, Long jobId) {
        validateSiteAccess(siteId, userId);

        CrawlJob crawlJob = crawlJobRepository.findById(jobId)
                .filter(job -> job.getSite().getId().equals(siteId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CRAWL_JOB_NOT_FOUND));

        return CrawlJobResponse.from(crawlJob);
    }

    @Transactional
    public CrawlJobResponse cancelCrawl(Long siteId, Long userId, Long jobId) {
        validateSiteAccess(siteId, userId);

        CrawlJob crawlJob = crawlJobRepository.findById(jobId)
                .filter(job -> job.getSite().getId().equals(siteId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CRAWL_JOB_NOT_FOUND));

        if (!crawlJob.isCancellable()) {
            throw new BusinessException(ErrorCode.CRAWL_JOB_NOT_FOUND,
                    "Crawl job cannot be cancelled in status: " + crawlJob.getStatus());
        }

        crawlJob.markCancelled();
        crawlExecutionService.cancelCrawl(jobId);

        log.info("Crawl job cancelled: id={}, siteId={}", jobId, siteId);
        return CrawlJobResponse.from(crawlJob);
    }

    @Transactional(readOnly = true)
    public PageResponse<CrawlResultResponse> getCrawlResults(Long siteId, Long userId,
                                                               Long jobId, Pageable pageable) {
        validateSiteAccess(siteId, userId);

        // Validate job belongs to site
        crawlJobRepository.findById(jobId)
                .filter(job -> job.getSite().getId().equals(siteId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CRAWL_JOB_NOT_FOUND));

        Page<CrawlResult> crawlResultPage = crawlResultRepository
                .findByCrawlJobIdOrderBySeoScoreAsc(jobId, pageable);

        // Batch-load all page analyses for this page of crawl results
        List<Long> crawlResultIds = crawlResultPage.getContent().stream()
                .map(CrawlResult::getId).toList();
        Map<Long, PageAnalysis> analysisByCrawlResultId = crawlResultIds.isEmpty()
                ? Map.of()
                : pageAnalysisRepository.findByCrawlResultIdIn(crawlResultIds).stream()
                    .collect(Collectors.toMap(pa -> pa.getCrawlResult().getId(), pa -> pa));

        Page<CrawlResultResponse> page = crawlResultPage
                .map(crawlResult -> CrawlResultResponse.from(
                        crawlResult, analysisByCrawlResultId.get(crawlResult.getId())));

        return PageResponse.from(page);
    }

    private void validateSiteAccess(Long siteId, Long userId) {
        siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));
    }
}
