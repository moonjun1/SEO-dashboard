package com.seodashboard.crawler.engine;

import com.seodashboard.crawler.config.CrawlerProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageFetcher {

    private final CrawlerProperties crawlerProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final long MAX_BODY_SIZE = 5 * 1024 * 1024; // 5MB

    public FetchResult fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", crawlerProperties.getUserAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .timeout(Duration.ofMillis(crawlerProperties.getRequestTimeoutMs()))
                    .GET()
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseTimeMs = (int) (System.currentTimeMillis() - startTime);

            // Reject responses larger than 5MB to prevent OOM
            if (response.body() != null && response.body().length() > MAX_BODY_SIZE) {
                log.warn("Response too large for {}: {} bytes", url, response.body().length());
                return new FetchResult(0, null, null, 0L, responseTimeMs, null,
                        "Response body exceeds maximum size of 5MB");
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("");

            long contentLength = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(response.body() != null ? response.body().length() : 0);

            // Detect redirect by comparing final URI with requested URI
            String finalUrl = response.uri().toString();
            String redirectUrl = finalUrl.equals(url) ? null : finalUrl;

            log.debug("Fetched {} -> status={}, time={}ms, size={}", url, response.statusCode(), responseTimeMs, contentLength);

            return new FetchResult(
                    response.statusCode(),
                    response.body(),
                    contentType,
                    contentLength,
                    responseTimeMs,
                    redirectUrl,
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to fetch {}: {}", url, e.getMessage());
            return new FetchResult(0, null, null, 0L, 0, null, e.getMessage());
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class FetchResult {
        private final int statusCode;
        private final String body;
        private final String contentType;
        private final long contentLength;
        private final int responseTimeMs;
        private final String redirectUrl;
        private final String error;

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 400 && body != null;
        }

        public boolean isHtml() {
            return contentType != null && contentType.contains("text/html");
        }
    }
}
