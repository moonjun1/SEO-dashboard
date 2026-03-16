package com.seodashboard.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordDensityAnalyzer {

    private final ObjectMapper objectMapper;

    /**
     * Analyzes keyword density for each target keyword in the content.
     *
     * @param content        the text content to analyze
     * @param targetKeywords list of target keywords
     * @return JSON string representing keyword density analysis
     */
    public String analyze(String content, List<String> targetKeywords) {
        if (targetKeywords == null || targetKeywords.isEmpty()) {
            return toJson(Map.of("keywords", List.of(), "totalWords", countWords(content)));
        }

        String lowerContent = content.toLowerCase();
        int totalWords = countWords(content);

        List<Map<String, Object>> keywordResults = new ArrayList<>();

        for (String keyword : targetKeywords) {
            String trimmed = keyword.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int count = countOccurrences(lowerContent, trimmed.toLowerCase());
            BigDecimal density = totalWords > 0
                    ? BigDecimal.valueOf(count * 100.0 / totalWords).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String status = classifyDensity(density);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("keyword", trimmed);
            entry.put("count", count);
            entry.put("density", density);
            entry.put("status", status);
            keywordResults.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalWords", totalWords);
        result.put("keywords", keywordResults);

        return toJson(result);
    }

    /**
     * Returns the average keyword density score (0-100).
     * Used by SeoContentScorer for composite scoring.
     */
    public BigDecimal calculateDensityScore(String content, List<String> targetKeywords) {
        if (targetKeywords == null || targetKeywords.isEmpty()) {
            return BigDecimal.valueOf(50); // Neutral score when no keywords specified
        }

        String lowerContent = content.toLowerCase();
        int totalWords = countWords(content);
        if (totalWords == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        int validKeywords = 0;

        for (String keyword : targetKeywords) {
            String trimmed = keyword.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int count = countOccurrences(lowerContent, trimmed.toLowerCase());
            BigDecimal density = BigDecimal.valueOf(count * 100.0 / totalWords).setScale(2, RoundingMode.HALF_UP);

            BigDecimal score;
            double d = density.doubleValue();
            if (d >= 1.0 && d <= 3.0) {
                score = BigDecimal.valueOf(100); // Optimal range
            } else if (d >= 0.5 && d < 1.0) {
                score = BigDecimal.valueOf(60); // Slightly low
            } else if (d > 3.0 && d <= 5.0) {
                score = BigDecimal.valueOf(60); // Slightly high
            } else if (d > 0 && d < 0.5) {
                score = BigDecimal.valueOf(30); // Very low
            } else if (d > 5.0) {
                score = BigDecimal.valueOf(20); // Keyword stuffing
            } else {
                score = BigDecimal.ZERO; // Keyword not present
            }

            totalScore = totalScore.add(score);
            validKeywords++;
        }

        if (validKeywords == 0) {
            return BigDecimal.valueOf(50);
        }

        return totalScore.divide(BigDecimal.valueOf(validKeywords), 2, RoundingMode.HALF_UP);
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String cleaned = content.replaceAll("#+ ", "").replaceAll("<[^>]+>", "");
        String[] words = cleaned.trim().split("\\s+");
        return words.length;
    }

    private int countOccurrences(String content, String keyword) {
        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String classifyDensity(BigDecimal density) {
        double d = density.doubleValue();
        if (d < 1.0) return "LOW";
        if (d <= 3.0) return "OPTIMAL";
        return "HIGH";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize keyword density result", e);
            return "{}";
        }
    }
}
