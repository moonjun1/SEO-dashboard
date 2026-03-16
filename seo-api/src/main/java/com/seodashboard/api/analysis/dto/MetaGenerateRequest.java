package com.seodashboard.api.analysis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MetaGenerateRequest(
        @NotBlank(message = "Content is required")
        String content,
        String targetKeywords,
        @Min(value = 1, message = "Count must be at least 1")
        @Max(value = 5, message = "Count must not exceed 5")
        Integer count
) {

    public int countOrDefault() {
        return count != null ? count : 3;
    }
}
