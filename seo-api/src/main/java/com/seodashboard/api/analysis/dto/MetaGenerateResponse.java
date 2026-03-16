package com.seodashboard.api.analysis.dto;

import com.seodashboard.ai.dto.MetaSuggestion;

import java.util.List;

public record MetaGenerateResponse(
        List<MetaSuggestionDto> suggestions
) {

    public record MetaSuggestionDto(
            String metaTitle,
            int metaTitleLength,
            String metaDescription,
            int metaDescriptionLength
    ) {
        public static MetaSuggestionDto from(MetaSuggestion suggestion) {
            return new MetaSuggestionDto(
                    suggestion.metaTitle(),
                    suggestion.metaTitleLength(),
                    suggestion.metaDescription(),
                    suggestion.metaDescriptionLength()
            );
        }
    }

    public static MetaGenerateResponse from(List<MetaSuggestion> suggestions) {
        List<MetaSuggestionDto> dtos = suggestions.stream()
                .map(MetaSuggestionDto::from)
                .toList();
        return new MetaGenerateResponse(dtos);
    }
}
