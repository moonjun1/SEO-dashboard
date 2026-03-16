package com.seodashboard.crawler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    private int maxConcurrentRequests = 5;
    private int requestTimeoutMs = 10000;
    private String userAgent = "SeoDashboard-Crawler/1.0";
    private int delayBetweenRequestsMs = 200;
}
