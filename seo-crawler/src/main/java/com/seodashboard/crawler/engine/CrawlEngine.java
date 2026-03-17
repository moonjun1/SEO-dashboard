package com.seodashboard.crawler.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.common.util.UrlUtils;
import com.seodashboard.crawler.analyzer.SeoAnalyzer;
import com.seodashboard.crawler.config.CrawlerProperties;
import com.seodashboard.crawler.dto.CrawlPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlEngine {

    private final PageFetcher pageFetcher;
    private final HtmlParser htmlParser;
    private final SeoAnalyzer seoAnalyzer;
    private final CrawlerProperties crawlerProperties;
    private final ObjectMapper objectMapper;

    public CrawlEngineResult crawl(String startUrl, int maxPages, int maxDepth, AtomicBoolean cancelFlag) {
        List<CrawlPageResult> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        String baseDomain = UrlUtils.extractDomain(startUrl);
        boolean hasSitemap = checkSitemap(baseDomain, startUrl);
        boolean hasRobotsTxt = checkRobotsTxt(baseDomain, startUrl);

        String normalizedStartUrl = UrlUtils.normalizeUrl(startUrl);
        visited.add(normalizedStartUrl);

        // BFS level-by-level: each level is fetched in parallel
        List<UrlWithDepth> currentLevel = List.of(new UrlWithDepth(normalizedStartUrl, 0));

        int concurrency = Math.max(1, crawlerProperties.getMaxConcurrentRequests());
        Semaphore semaphore = new Semaphore(concurrency);
        long delayMs = crawlerProperties.getDelayBetweenRequestsMs();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (!currentLevel.isEmpty() && results.size() < maxPages) {
                if (cancelFlag.get()) {
                    log.info("Crawl cancelled. Processed {} pages so far.", results.size());
                    break;
                }

                // Cap the batch to remaining page budget
                int remaining = maxPages - results.size();
                List<UrlWithDepth> batch = currentLevel.size() <= remaining
                        ? currentLevel
                        : currentLevel.subList(0, remaining);

                // Submit parallel fetch tasks for this BFS level
                List<CompletableFuture<PageFetchOutcome>> futures = new ArrayList<>(batch.size());
                for (UrlWithDepth item : batch) {
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        if (cancelFlag.get()) {
                            return null;
                        }
                        try {
                            semaphore.acquire();
                            try {
                                CrawlPageResult pageResult = processPage(
                                        item.url(), item.depth(), baseDomain, hasSitemap, hasRobotsTxt);
                                // Per-thread delay to be polite to the target server
                                if (delayMs > 0) {
                                    Thread.sleep(delayMs);
                                }
                                return new PageFetchOutcome(item, pageResult);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("Crawl interrupted at URL: {}", item.url());
                            return null;
                        } catch (Exception e) {
                            log.warn("Error processing URL {}: {}", item.url(), e.getMessage());
                            errorCount.incrementAndGet();
                            return null;
                        }
                    }, executor));
                }

                // Collect results from this level and discover next-level URLs
                List<UrlWithDepth> nextLevel = new ArrayList<>();
                for (CompletableFuture<PageFetchOutcome> future : futures) {
                    if (cancelFlag.get()) {
                        break;
                    }
                    try {
                        PageFetchOutcome outcome = future.join();
                        if (outcome == null) {
                            continue;
                        }
                        CrawlPageResult pageResult = outcome.result();
                        if (pageResult != null) {
                            results.add(pageResult);
                            // Enqueue internal links if within depth limit
                            int nextDepth = outcome.source().depth() + 1;
                            if (nextDepth <= maxDepth && pageResult.getInternalLinks() != null) {
                                for (String link : pageResult.getInternalLinks()) {
                                    String normalized = UrlUtils.normalizeUrl(link);
                                    if (!visited.contains(normalized) && visited.size() < maxPages * 2) {
                                        visited.add(normalized);
                                        nextLevel.add(new UrlWithDepth(normalized, nextDepth));
                                    }
                                }
                            }
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.warn("Error collecting crawl result: {}", e.getMessage());
                        errorCount.incrementAndGet();
                    }
                }

                currentLevel = nextLevel;
            }
        }

        log.info("Crawl completed. Pages: {}, Errors: {}", results.size(), errorCount.get());
        return new CrawlEngineResult(results, errorCount.get());
    }

    private record PageFetchOutcome(UrlWithDepth source, CrawlPageResult result) {}

    private CrawlPageResult processPage(String url, int depth, String baseDomain,
                                          boolean hasSitemap, boolean hasRobotsTxt) {
        PageFetcher.FetchResult fetchResult = pageFetcher.fetch(url);

        if (!fetchResult.isSuccess()) {
            log.debug("Failed to fetch {}: status={}, error={}", url, fetchResult.getStatusCode(), fetchResult.getError());
            return null;
        }

        if (!fetchResult.isHtml()) {
            log.debug("Skipping non-HTML content at {}: {}", url, fetchResult.getContentType());
            return null;
        }

        // Parse HTML
        HtmlParser.ParseResult parseResult = htmlParser.parse(fetchResult.getBody(), url);

        // Analyze SEO
        SeoAnalyzer.AnalysisResult analysis = seoAnalyzer.analyze(parseResult, fetchResult.getResponseTimeMs(), 0);

        // Serialize issues to JSON
        String issuesJson;
        try {
            issuesJson = objectMapper.writeValueAsString(analysis.getIssues());
        } catch (JsonProcessingException e) {
            issuesJson = "[]";
        }

        return CrawlPageResult.builder()
                .url(url)
                .statusCode(fetchResult.getStatusCode())
                .contentType(fetchResult.getContentType())
                .contentLength(fetchResult.getContentLength())
                .responseTimeMs(fetchResult.getResponseTimeMs())
                .depth(depth)
                .isInternal(true)
                .redirectUrl(fetchResult.getRedirectUrl())
                .title(parseResult.getTitle())
                .metaDescription(parseResult.getMetaDescription())
                .canonicalUrl(parseResult.getCanonicalUrl())
                .internalLinks(parseResult.getInternalLinks())
                .externalLinks(parseResult.getExternalLinks())
                .seoScore(analysis.getSeoScore())
                .titleScore(analysis.getTitleScore())
                .titleLength(analysis.getTitleLength())
                .metaDescriptionScore(analysis.getMetaDescriptionScore())
                .metaDescriptionLength(analysis.getMetaDescriptionLength())
                .headingScore(analysis.getHeadingScore())
                .headingStructure(analysis.getHeadingStructure())
                .imageScore(analysis.getImageScore())
                .imagesTotal(analysis.getImagesTotal())
                .imagesWithoutAlt(analysis.getImagesWithoutAlt())
                .linkScore(analysis.getLinkScore())
                .internalLinksCount(analysis.getInternalLinksCount())
                .externalLinksCount(analysis.getExternalLinksCount())
                .brokenLinksCount(analysis.getBrokenLinksCount())
                .performanceScore(analysis.getPerformanceScore())
                .hasOgTags(analysis.getHasOgTags())
                .hasTwitterCards(analysis.getHasTwitterCards())
                .hasStructuredData(analysis.getHasStructuredData())
                .hasSitemap(hasSitemap)
                .hasRobotsTxt(hasRobotsTxt)
                .isMobileFriendly(analysis.getIsMobileFriendly())
                .issues(analysis.getIssues())
                .build();
    }

    private boolean checkRobotsTxt(String domain, String startUrl) {
        try {
            String scheme = URI.create(startUrl).getScheme();
            String robotsUrl = scheme + "://" + domain + "/robots.txt";
            PageFetcher.FetchResult result = pageFetcher.fetch(robotsUrl);
            return result.getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkSitemap(String domain, String startUrl) {
        try {
            String scheme = URI.create(startUrl).getScheme();
            String sitemapUrl = scheme + "://" + domain + "/sitemap.xml";
            PageFetcher.FetchResult result = pageFetcher.fetch(sitemapUrl);
            return result.getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private record UrlWithDepth(String url, int depth) {}

    public record CrawlEngineResult(List<CrawlPageResult> pages, int errorCount) {}
}
