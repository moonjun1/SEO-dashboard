package com.seodashboard.crawler.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.crawler.analyzer.SeoAnalyzer;
import com.seodashboard.crawler.config.CrawlerProperties;
import com.seodashboard.crawler.dto.CrawlPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Queue<UrlWithDepth> queue = new ArrayDeque<>();
        int errorCount = 0;

        String baseDomain = extractDomain(startUrl);
        boolean hasSitemap = checkSitemap(baseDomain, startUrl);
        boolean hasRobotsTxt = checkRobotsTxt(baseDomain, startUrl);

        String normalizedStartUrl = normalizeUrl(startUrl);
        queue.add(new UrlWithDepth(normalizedStartUrl, 0));
        visited.add(normalizedStartUrl);

        while (!queue.isEmpty() && results.size() < maxPages) {
            if (cancelFlag.get()) {
                log.info("Crawl cancelled. Processed {} pages so far.", results.size());
                break;
            }

            UrlWithDepth current = queue.poll();

            try {
                CrawlPageResult pageResult = processPage(current.url(), current.depth(),
                        baseDomain, hasSitemap, hasRobotsTxt);

                if (pageResult != null) {
                    results.add(pageResult);

                    // Enqueue internal links if within depth limit
                    if (current.depth() < maxDepth && pageResult.getInternalLinks() != null) {
                        for (String link : pageResult.getInternalLinks()) {
                            String normalized = normalizeUrl(link);
                            if (!visited.contains(normalized) && visited.size() < maxPages * 2) {
                                visited.add(normalized);
                                queue.add(new UrlWithDepth(normalized, current.depth() + 1));
                            }
                        }
                    }
                } else {
                    errorCount++;
                }

                // Delay between requests
                if (!queue.isEmpty() && crawlerProperties.getDelayBetweenRequestsMs() > 0) {
                    Thread.sleep(crawlerProperties.getDelayBetweenRequestsMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Crawl interrupted at URL: {}", current.url());
                break;
            } catch (Exception e) {
                log.warn("Error processing URL {}: {}", current.url(), e.getMessage());
                errorCount++;
            }
        }

        log.info("Crawl completed. Pages: {}, Errors: {}", results.size(), errorCount);
        return new CrawlEngineResult(results, errorCount);
    }

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

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() != null ? uri.getHost().toLowerCase() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeUrl(String url) {
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex > 0) {
            url = url.substring(0, fragmentIndex);
        }
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private record UrlWithDepth(String url, int depth) {}

    public record CrawlEngineResult(List<CrawlPageResult> pages, int errorCount) {}
}
