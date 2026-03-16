package com.seodashboard.api.site.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record SiteUpdateRequest(

        @URL(message = "Invalid URL format")
        String url,

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @Min(value = 24, message = "Crawl interval must be at least 24 hours")
        @Max(value = 720, message = "Crawl interval must not exceed 720 hours")
        Integer crawlIntervalHours
) {
}
