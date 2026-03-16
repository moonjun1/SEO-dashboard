package com.seodashboard.api.keyword.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeywordCreateRequest(

        @NotBlank(message = "Keyword is required")
        @Size(max = 500, message = "Keyword must not exceed 500 characters")
        String keyword,

        @Size(max = 2048, message = "Target URL must not exceed 2048 characters")
        String targetUrl,

        String searchEngine,

        String countryCode,

        String languageCode
) {

    public String searchEngineOrDefault() {
        return searchEngine != null ? searchEngine : "GOOGLE";
    }

    public String countryCodeOrDefault() {
        return countryCode != null ? countryCode : "KR";
    }

    public String languageCodeOrDefault() {
        return languageCode != null ? languageCode : "ko";
    }
}
