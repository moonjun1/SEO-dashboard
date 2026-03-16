package com.seodashboard.api.publicseo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.api.publicseo.dto.PublicAnalysisListResponse;
import com.seodashboard.api.publicseo.dto.PublicAnalysisResponse;
import com.seodashboard.api.publicseo.repository.PublicAnalysisRepository;
import com.seodashboard.common.domain.PublicAnalysis;
import com.seodashboard.crawler.analyzer.SeoAnalyzer;
import com.seodashboard.crawler.engine.HtmlParser;
import com.seodashboard.crawler.engine.PageFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicSeoService {

    private final PageFetcher pageFetcher;
    private final HtmlParser htmlParser;
    private final SeoAnalyzer seoAnalyzer;
    private final PublicAnalysisRepository publicAnalysisRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PublicAnalysisResponse analyze(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        String domain = extractDomain(url);

        // Create initial record with ANALYZING status
        PublicAnalysis analysis = PublicAnalysis.builder()
                .url(url)
                .domain(domain)
                .status("ANALYZING")
                .build();
        analysis = publicAnalysisRepository.save(analysis);

        try {
            // 1. Fetch the page
            PageFetcher.FetchResult fetchResult = pageFetcher.fetch(url);
            if (!fetchResult.isSuccess()) {
                String error = fetchResult.getError() != null
                        ? fetchResult.getError()
                        : "HTTP " + fetchResult.getStatusCode();
                analysis.markFailed("Failed to fetch URL: " + error);
                publicAnalysisRepository.save(analysis);
                return toResponse(analysis);
            }

            if (!fetchResult.isHtml()) {
                analysis.markFailed("URL does not return HTML content: " + fetchResult.getContentType());
                publicAnalysisRepository.save(analysis);
                return toResponse(analysis);
            }

            String htmlBody = fetchResult.getBody();
            int responseTimeMs = fetchResult.getResponseTimeMs();
            int contentLength = (int) fetchResult.getContentLength();

            // 2. Parse HTML with existing HtmlParser
            HtmlParser.ParseResult parseResult = htmlParser.parse(htmlBody, url);

            // 3. Analyze SEO with existing SeoAnalyzer
            SeoAnalyzer.AnalysisResult seoResult = seoAnalyzer.analyze(parseResult, responseTimeMs, 0);

            // 4. Additional analysis using Jsoup directly
            Document doc = Jsoup.parse(htmlBody, url);

            // Collect all meta tags
            List<Map<String, String>> metaTagsList = collectMetaTags(doc);
            String metaTagsJson = toJson(metaTagsList);

            // Collect link list
            List<Map<String, Object>> linkListData = collectLinks(doc, domain);
            String linkListJson = toJson(linkListData);

            // Check favicon
            boolean hasFavicon = checkFavicon(doc, url);

            // Check HTTPS
            boolean hasHttps = url.startsWith("https://");

            // Check viewport (mobile friendliness)
            boolean hasViewport = !doc.select("meta[name=viewport]").isEmpty();

            // Check robots.txt and sitemap.xml
            String scheme = URI.create(url).getScheme();
            boolean hasRobotsTxt = checkResource(scheme + "://" + domain + "/robots.txt");
            boolean hasSitemap = checkResource(scheme + "://" + domain + "/sitemap.xml");

            // Serialize issues and heading structure
            String issuesJson = toJson(seoResult.getIssues());
            String headingStructureJson = seoResult.getHeadingStructure();

            // 5. Update the entity with results
            analysis.markCompleted(
                    seoResult.getSeoScore(),
                    seoResult.getTitleScore(),
                    seoResult.getMetaDescriptionScore(),
                    seoResult.getHeadingScore(),
                    seoResult.getImageScore(),
                    seoResult.getLinkScore(),
                    seoResult.getPerformanceScore(),
                    parseResult.getTitle(),
                    parseResult.getMetaDescription(),
                    parseResult.getCanonicalUrl(),
                    responseTimeMs,
                    contentLength,
                    parseResult.getImagesTotal(),
                    parseResult.getImagesWithoutAlt(),
                    seoResult.getInternalLinksCount(),
                    seoResult.getExternalLinksCount(),
                    seoResult.getBrokenLinksCount(),
                    parseResult.getHeadings().size(),
                    parseResult.isHasOgTags(),
                    parseResult.isHasTwitterCards(),
                    hasViewport,
                    hasFavicon,
                    hasRobotsTxt,
                    hasSitemap,
                    hasHttps,
                    headingStructureJson,
                    issuesJson,
                    linkListJson,
                    metaTagsJson
            );

            analysis = publicAnalysisRepository.save(analysis);
            log.info("Public SEO analysis completed for URL: {} (score: {})", url, seoResult.getSeoScore());

            return toResponse(analysis);

        } catch (Exception e) {
            log.error("Public SEO analysis failed for URL: {}", url, e);
            analysis.markFailed("Analysis failed: " + e.getMessage());
            publicAnalysisRepository.save(analysis);
            return toResponse(analysis);
        }
    }

    @Transactional(readOnly = true)
    public PublicAnalysisResponse getAnalysis(Long id) {
        PublicAnalysis analysis = publicAnalysisRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + id));
        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public List<PublicAnalysisListResponse> getRecentAnalyses() {
        return publicAnalysisRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(PublicAnalysisListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicAnalysisListResponse> getRanking() {
        return publicAnalysisRepository
                .findByStatusOrderBySeoScoreDesc("COMPLETED", PageRequest.of(0, 50))
                .stream()
                .map(PublicAnalysisListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicAnalysisListResponse> getDomainHistory(String domain) {
        return publicAnalysisRepository
                .findByDomainOrderByCreatedAtDesc(domain, PageRequest.of(0, 50))
                .stream()
                .map(PublicAnalysisListResponse::from)
                .toList();
    }

    // --- Private helpers ---

    private List<Map<String, String>> collectMetaTags(Document doc) {
        List<Map<String, String>> metaTags = new ArrayList<>();
        Elements metas = doc.select("meta");
        for (Element meta : metas) {
            String name = meta.attr("name");
            String property = meta.attr("property");
            String content = meta.attr("content");
            String httpEquiv = meta.attr("http-equiv");
            String charset = meta.attr("charset");

            if (!name.isEmpty() || !property.isEmpty() || !httpEquiv.isEmpty() || !charset.isEmpty()) {
                Map<String, String> tag = new LinkedHashMap<>();
                if (!name.isEmpty()) tag.put("name", name);
                if (!property.isEmpty()) tag.put("property", property);
                if (!content.isEmpty()) tag.put("content", content);
                if (!httpEquiv.isEmpty()) tag.put("httpEquiv", httpEquiv);
                if (!charset.isEmpty()) tag.put("charset", charset);
                metaTags.add(tag);
            }
        }
        return metaTags;
    }

    private List<Map<String, Object>> collectLinks(Document doc, String baseDomain) {
        List<Map<String, Object>> links = new ArrayList<>();
        Elements anchors = doc.select("a[href]");
        for (Element anchor : anchors) {
            String href = anchor.absUrl("href");
            if (href.isEmpty() || href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("tel:")) {
                continue;
            }
            String text = anchor.text();
            boolean isInternal = href.contains(baseDomain);
            Map<String, Object> linkEntry = new LinkedHashMap<>();
            linkEntry.put("href", href);
            linkEntry.put("text", text);
            linkEntry.put("internal", isInternal);
            links.add(linkEntry);
        }
        return links;
    }

    private boolean checkFavicon(Document doc, String url) {
        // Check HTML link tags for favicon
        boolean hasInHtml = !doc.select("link[rel=icon], link[rel='shortcut icon'], link[rel=apple-touch-icon]").isEmpty();
        if (hasInHtml) {
            return true;
        }
        // Fallback: check /favicon.ico
        try {
            String scheme = URI.create(url).getScheme();
            String host = URI.create(url).getHost();
            String faviconUrl = scheme + "://" + host + "/favicon.ico";
            PageFetcher.FetchResult result = pageFetcher.fetch(faviconUrl);
            return result.getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkResource(String resourceUrl) {
        try {
            PageFetcher.FetchResult result = pageFetcher.fetch(resourceUrl);
            return result.getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeUrl(String rawUrl) {
        String url = rawUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        // Remove trailing slash
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON", e);
            return "[]";
        }
    }

    private PublicAnalysisResponse toResponse(PublicAnalysis entity) {
        Object headingStructure = parseJsonSafe(entity.getHeadingStructure());
        Object issues = parseJsonSafe(entity.getIssues());
        Object linkList = parseJsonSafe(entity.getLinkList());
        Object metaTags = parseJsonSafe(entity.getMetaTags());

        return PublicAnalysisResponse.from(entity, headingStructure, issues, linkList, metaTags);
    }

    private Object parseJsonSafe(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
