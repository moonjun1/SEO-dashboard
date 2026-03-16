package com.seodashboard.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.ai.client.AiClient;
import com.seodashboard.ai.dto.AiAnalysisResult;
import com.seodashboard.common.domain.ContentAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAnalysisExecutor {

    private final KeywordDensityAnalyzer keywordDensityAnalyzer;
    private final ReadabilityAnalyzer readabilityAnalyzer;
    private final SeoContentScorer seoContentScorer;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ContentAnalysisJpaRepository contentAnalysisRepository;

    /**
     * Executes the full content analysis pipeline:
     * 1) Keyword density analysis (local)
     * 2) Readability analysis (local)
     * 3) SEO content scoring (local)
     * 4) AI-driven suggestions and meta generation
     * 5) Persists results to ContentAnalysis entity
     */
    @Transactional
    public void execute(Long analysisId) {
        ContentAnalysis analysis = contentAnalysisRepository.findById(analysisId)
                .orElse(null);

        if (analysis == null) {
            log.error("ContentAnalysis not found: id={}", analysisId);
            return;
        }

        log.info("Starting content analysis: id={}", analysisId);
        analysis.markProcessing();
        contentAnalysisRepository.save(analysis);

        try {
            String content = analysis.getContent();
            List<String> targetKeywords = parseKeywords(analysis.getTargetKeywords());

            // Step 1: Keyword density analysis
            String keywordDensityJson = keywordDensityAnalyzer.analyze(content, targetKeywords);

            // Step 2: Readability analysis + structure
            String structureAnalysisJson = readabilityAnalyzer.analyzeStructure(content);
            BigDecimal readabilityScore = readabilityAnalyzer.calculateReadabilityScore(content);

            // Step 3: AI-driven analysis (suggestions + meta)
            AiAnalysisResult aiResult = aiClient.analyzeContent(content, targetKeywords);

            // Step 4: Calculate composite SEO score
            boolean hasMetaTitle = aiResult.generatedMetaTitle() != null && !aiResult.generatedMetaTitle().isEmpty();
            boolean hasMetaDesc = aiResult.generatedMetaDescription() != null && !aiResult.generatedMetaDescription().isEmpty();
            BigDecimal seoScore = seoContentScorer.calculateScore(content, targetKeywords, hasMetaTitle, hasMetaDesc);

            // Step 5: Serialize suggestions
            String suggestionsJson = toJson(aiResult.suggestions());

            // Step 6: Update entity
            analysis.markCompleted(
                    seoScore,
                    readabilityScore,
                    keywordDensityJson,
                    structureAnalysisJson,
                    suggestionsJson,
                    aiResult.generatedMetaTitle(),
                    aiResult.generatedMetaDescription(),
                    aiResult.aiProvider(),
                    aiResult.aiModel()
            );
            contentAnalysisRepository.save(analysis);

            log.info("Content analysis completed: id={}, seoScore={}, readabilityScore={}",
                    analysisId, seoScore, readabilityScore);

        } catch (Exception e) {
            log.error("Content analysis failed: id={}", analysisId, e);
            analysis.markFailed(e.getMessage());
            contentAnalysisRepository.save(analysis);
        }
    }

    private List<String> parseKeywords(String targetKeywords) {
        if (targetKeywords == null || targetKeywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(targetKeywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return "[]";
        }
    }

    /**
     * Internal repository interface to be satisfied by the API module's repository bean.
     * This allows seo-ai to remain a library without directly declaring repository scanning.
     */
    public interface ContentAnalysisJpaRepository extends JpaRepository<ContentAnalysis, Long> {
    }
}
