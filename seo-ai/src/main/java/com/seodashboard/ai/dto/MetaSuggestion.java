package com.seodashboard.ai.dto;

public record MetaSuggestion(
        String metaTitle,
        int metaTitleLength,
        String metaDescription,
        int metaDescriptionLength
) {
}
