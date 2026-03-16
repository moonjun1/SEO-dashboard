package com.seodashboard.crawler.dto;

import java.util.Map;

public record SeoIssue(
        String type,
        String severity,
        String message,
        Map<String, Object> details
) {

    public static SeoIssue error(String type, String message) {
        return new SeoIssue(type, "ERROR", message, null);
    }

    public static SeoIssue error(String type, String message, Map<String, Object> details) {
        return new SeoIssue(type, "ERROR", message, details);
    }

    public static SeoIssue warning(String type, String message) {
        return new SeoIssue(type, "WARNING", message, null);
    }

    public static SeoIssue warning(String type, String message, Map<String, Object> details) {
        return new SeoIssue(type, "WARNING", message, details);
    }

    public static SeoIssue info(String type, String message) {
        return new SeoIssue(type, "INFO", message, null);
    }
}
