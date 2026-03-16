package com.seodashboard.api.keyword.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record KeywordBatchCreateRequest(

        @NotEmpty(message = "Keywords list must not be empty")
        List<@Valid KeywordItem> keywords,

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

    public record KeywordItem(

            @NotBlank(message = "Keyword is required")
            @Size(max = 500, message = "Keyword must not exceed 500 characters")
            String keyword,

            @Size(max = 2048, message = "Target URL must not exceed 2048 characters")
            String targetUrl
    ) {}
}
