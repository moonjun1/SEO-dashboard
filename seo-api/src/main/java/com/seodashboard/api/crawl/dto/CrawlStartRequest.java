package com.seodashboard.api.crawl.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CrawlStartRequest(

        @Min(value = 1, message = "Max pages must be at least 1")
        @Max(value = 1000, message = "Max pages must not exceed 1000")
        Integer maxPages,

        @Min(value = 1, message = "Max depth must be at least 1")
        @Max(value = 10, message = "Max depth must not exceed 10")
        Integer maxDepth
) {

    public int maxPagesOrDefault() {
        return maxPages != null ? maxPages : 100;
    }

    public int maxDepthOrDefault() {
        return maxDepth != null ? maxDepth : 3;
    }
}
