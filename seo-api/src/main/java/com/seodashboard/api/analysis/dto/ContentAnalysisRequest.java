package com.seodashboard.api.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentAnalysisRequest(
        Long siteId,
        String title,
        @NotBlank(message = "Content is required")
        @Size(min = 100, max = 50000, message = "Content must be between 100 and 50000 characters")
        String content,
        @Size(max = 500, message = "Target keywords must not exceed 500 characters")
        String targetKeywords // comma-separated, max 10
) {
}
