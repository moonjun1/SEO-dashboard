package com.seodashboard.ai.dto;

import java.util.List;

public record AiAnalysisResult(
        List<ContentSuggestion> suggestions,
        String generatedMetaTitle,
        String generatedMetaDescription,
        String aiProvider,
        String aiModel
) {
}
