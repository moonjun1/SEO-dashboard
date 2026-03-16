package com.seodashboard.api.publicseo.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicAnalyzeRequest(
        @NotBlank(message = "URL is required")
        String url
) {}
