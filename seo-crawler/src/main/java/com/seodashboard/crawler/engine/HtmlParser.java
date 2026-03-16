package com.seodashboard.crawler.engine;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HtmlParser {

    public ParseResult parse(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);

        String title = doc.title();

        String metaDescription = getMetaContent(doc, "description");
        String canonicalUrl = getCanonicalUrl(doc);

        // OG tags
        boolean hasOgTags = doc.select("meta[property^=og:]").size() >= 2;

        // Twitter cards
        boolean hasTwitterCards = doc.select("meta[name^=twitter:]").size() >= 2;

        // Structured data (JSON-LD, Microdata, RDFa)
        boolean hasStructuredData = !doc.select("script[type=application/ld+json]").isEmpty()
                || !doc.select("[itemscope]").isEmpty()
                || !doc.select("[vocab]").isEmpty();

        // Viewport meta tag (mobile-friendly indicator)
        boolean isMobileFriendly = !doc.select("meta[name=viewport]").isEmpty();

        // Headings
        List<HeadingInfo> headings = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Elements hElements = doc.select("h" + i);
            for (Element h : hElements) {
                headings.add(new HeadingInfo("h" + i, h.text()));
            }
        }

        // Images
        Elements images = doc.select("img");
        int imagesTotal = images.size();
        int imagesWithoutAlt = 0;
        for (Element img : images) {
            String alt = img.attr("alt");
            if (alt == null || alt.isBlank()) {
                imagesWithoutAlt++;
            }
        }

        // Links
        List<String> internalLinks = new ArrayList<>();
        List<String> externalLinks = new ArrayList<>();
        String baseDomain = extractDomain(baseUrl);

        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.absUrl("href");
            if (href.isEmpty() || href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("tel:")) {
                continue;
            }
            String linkDomain = extractDomain(href);
            if (baseDomain.equals(linkDomain)) {
                internalLinks.add(normalizeUrl(href));
            } else if (href.startsWith("http")) {
                externalLinks.add(href);
            }
        }

        return ParseResult.builder()
                .title(title)
                .metaDescription(metaDescription)
                .canonicalUrl(canonicalUrl)
                .hasOgTags(hasOgTags)
                .hasTwitterCards(hasTwitterCards)
                .hasStructuredData(hasStructuredData)
                .isMobileFriendly(isMobileFriendly)
                .headings(headings)
                .imagesTotal(imagesTotal)
                .imagesWithoutAlt(imagesWithoutAlt)
                .internalLinks(internalLinks)
                .externalLinks(externalLinks)
                .build();
    }

    private String getMetaContent(Document doc, String name) {
        Element meta = doc.selectFirst("meta[name=" + name + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[property=" + name + "]");
        }
        return meta != null ? meta.attr("content") : null;
    }

    private String getCanonicalUrl(Document doc) {
        Element canonical = doc.selectFirst("link[rel=canonical]");
        return canonical != null ? canonical.attr("abs:href") : null;
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

    private String normalizeUrl(String url) {
        // Remove fragment
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex > 0) {
            url = url.substring(0, fragmentIndex);
        }
        // Remove trailing slash
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Getter
    @Builder
    public static class ParseResult {
        private final String title;
        private final String metaDescription;
        private final String canonicalUrl;
        private final boolean hasOgTags;
        private final boolean hasTwitterCards;
        private final boolean hasStructuredData;
        private final boolean isMobileFriendly;
        private final List<HeadingInfo> headings;
        private final int imagesTotal;
        private final int imagesWithoutAlt;
        private final List<String> internalLinks;
        private final List<String> externalLinks;
    }

    public record HeadingInfo(String level, String text) {}
}
