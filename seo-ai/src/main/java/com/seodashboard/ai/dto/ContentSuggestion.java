package com.seodashboard.ai.dto;

public record ContentSuggestion(
        String category,
        String severity,
        String message,
        String recommendation
) {
}
