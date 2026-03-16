package com.seodashboard.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoContentScorer {

    private final KeywordDensityAnalyzer keywordDensityAnalyzer;
    private final ReadabilityAnalyzer readabilityAnalyzer;

    /**
     * Calculates composite SEO score (0-100) based on:
     * - Keyword density appropriateness (30%)
     * - Readability (25%)
     * - Content length appropriateness (20%)
     * - Structure (15%)
     * - Meta information (10%)
     *
     * @param content         the text content
     * @param targetKeywords  list of target keywords
     * @param hasMetaTitle    whether meta title is available
     * @param hasMetaDesc     whether meta description is available
     * @return composite SEO score
     */
    public BigDecimal calculateScore(String content, List<String> targetKeywords,
                                     boolean hasMetaTitle, boolean hasMetaDesc) {
        BigDecimal keywordScore = keywordDensityAnalyzer.calculateDensityScore(content, targetKeywords);
        BigDecimal readabilityScore = readabilityAnalyzer.calculateReadabilityScore(content);
        BigDecimal lengthScore = calculateLengthScore(content);
        BigDecimal structureScore = calculateStructureScore(content);
        BigDecimal metaScore = calculateMetaScore(hasMetaTitle, hasMetaDesc);

        // Weighted composite
        BigDecimal composite = BigDecimal.ZERO;
        composite = composite.add(keywordScore.multiply(BigDecimal.valueOf(0.30)));
        composite = composite.add(readabilityScore.multiply(BigDecimal.valueOf(0.25)));
        composite = composite.add(lengthScore.multiply(BigDecimal.valueOf(0.20)));
        composite = composite.add(structureScore.multiply(BigDecimal.valueOf(0.15)));
        composite = composite.add(metaScore.multiply(BigDecimal.valueOf(0.10)));

        BigDecimal result = composite.setScale(2, RoundingMode.HALF_UP);

        log.debug("SEO Score breakdown: keyword={}, readability={}, length={}, structure={}, meta={}, total={}",
                keywordScore, readabilityScore, lengthScore, structureScore, metaScore, result);

        return result;
    }

    /**
     * Content length scoring (0-100).
     * Optimal: 300-3000 words.
     */
    private BigDecimal calculateLengthScore(String content) {
        int wordCount = countWords(content);

        if (wordCount >= 300 && wordCount <= 3000) {
            return BigDecimal.valueOf(100);
        } else if (wordCount >= 200 && wordCount < 300) {
            return BigDecimal.valueOf(70);
        } else if (wordCount > 3000 && wordCount <= 5000) {
            return BigDecimal.valueOf(80);
        } else if (wordCount >= 100 && wordCount < 200) {
            return BigDecimal.valueOf(40);
        } else if (wordCount > 5000) {
            return BigDecimal.valueOf(60);
        } else {
            return BigDecimal.valueOf(20); // Very short content
        }
    }

    /**
     * Structure scoring (0-100): subheading usage and paragraph division.
     */
    private BigDecimal calculateStructureScore(String content) {
        BigDecimal score = BigDecimal.valueOf(100);
        int wordCount = countWords(content);

        // Check for headings
        boolean hasHeadings = content.contains("##") || content.matches("(?s).*<h[1-6].*");
        if (!hasHeadings && wordCount > 200) {
            score = score.subtract(BigDecimal.valueOf(40));
        } else if (!hasHeadings) {
            score = score.subtract(BigDecimal.valueOf(20));
        }

        // Check paragraph division
        String[] paragraphs = content.split("\n\\s*\n");
        int paragraphCount = 0;
        for (String p : paragraphs) {
            if (!p.trim().isEmpty()) {
                paragraphCount++;
            }
        }

        if (paragraphCount < 3 && wordCount > 200) {
            score = score.subtract(BigDecimal.valueOf(30));
        } else if (paragraphCount < 2) {
            score = score.subtract(BigDecimal.valueOf(20));
        }

        // Check for lists (bonus)
        if (content.contains("- ") || content.contains("* ") || content.contains("<li")) {
            score = score.add(BigDecimal.valueOf(10));
        }

        // Clamp
        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) score = BigDecimal.valueOf(100);

        return score;
    }

    /**
     * Meta information scoring (0-100).
     */
    private BigDecimal calculateMetaScore(boolean hasMetaTitle, boolean hasMetaDesc) {
        int score = 0;
        if (hasMetaTitle) score += 50;
        if (hasMetaDesc) score += 50;
        return BigDecimal.valueOf(score);
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String cleaned = content.replaceAll("#+ ", "").replaceAll("<[^>]+>", "");
        return cleaned.trim().split("\\s+").length;
    }
}
