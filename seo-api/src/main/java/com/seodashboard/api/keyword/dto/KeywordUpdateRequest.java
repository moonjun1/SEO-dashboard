package com.seodashboard.api.keyword.dto;

import jakarta.validation.constraints.Size;

public record KeywordUpdateRequest(

        @Size(max = 2048, message = "Target URL must not exceed 2048 characters")
        String targetUrl,

        Boolean isActive
) {}
