package com.seodashboard.api.analysis.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.common.domain.ContentAnalysis;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
public record ContentAnalysisResponse(
        Long id,
        Long siteId,
        String title,
        String targetKeywords,
        BigDecimal seoScore,
        BigDecimal readabilityScore,
        JsonNode keywordDensity,
        JsonNode structureAnalysis,
        JsonNode suggestions,
        String generatedMetaTitle,
        String generatedMetaDescription,
        String aiProvider,
        String aiModel,
        String status,
        String errorMessage,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ContentAnalysisResponse from(ContentAnalysis analysis) {
        return new ContentAnalysisResponse(
                analysis.getId(),
                analysis.getSite() != null ? analysis.getSite().getId() : null,
                analysis.getTitle(),
                analysis.getTargetKeywords(),
                analysis.getSeoScore(),
                analysis.getReadabilityScore(),
                parseJson(analysis.getKeywordDensity()),
                parseJson(analysis.getStructureAnalysis()),
                parseJson(analysis.getSuggestions()),
                analysis.getGeneratedMetaTitle(),
                analysis.getGeneratedMetaDescription(),
                analysis.getAiProvider(),
                analysis.getAiModel(),
                analysis.getStatus(),
                analysis.getErrorMessage(),
                analysis.getCompletedAt(),
                analysis.getCreatedAt()
        );
    }

    private static JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse JSON field: {}", e.getMessage());
            return null;
        }
    }
}
