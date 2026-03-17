package com.seodashboard.api.publicseo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicAnalyzeRequest(
        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String url
) {}
