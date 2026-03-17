package com.seodashboard.common.util;

import java.net.URI;

/**
 * Shared URL normalization and domain extraction utilities.
 * Eliminates duplication between PublicSeoService and CrawlEngine.
 */
public final class UrlUtils {

    private UrlUtils() {
        // utility class
    }

    /**
     * Normalizes a URL by stripping the fragment and trailing slash.
     * Does NOT prepend a scheme -- callers that accept raw user input
     * should add "https://" before calling this method.
     */
    public static String normalizeUrl(String url) {
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex > 0) {
            url = url.substring(0, fragmentIndex);
        }
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Ensures the URL has an http(s) scheme, then normalizes it.
     * Suitable for raw user input that may omit the scheme.
     */
    public static String normalizeUrlWithScheme(String rawUrl) {
        String url = rawUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return normalizeUrl(url);
    }

    /**
     * Extracts the lowercase hostname from a URL.
     * Returns an empty string on parse failure.
     */
    public static String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
