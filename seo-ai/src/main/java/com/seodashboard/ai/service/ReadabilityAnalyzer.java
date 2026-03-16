package com.seodashboard.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadabilityAnalyzer {

    private final ObjectMapper objectMapper;

    /**
     * Analyzes readability of the content and returns a JSON structure analysis.
     *
     * @param content the text content to analyze
     * @return JSON string representing structure analysis
     */
    public String analyzeStructure(String content) {
        int wordCount = countWords(content);
        int sentenceCount = countSentences(content);
        int paragraphCount = countParagraphs(content);
        int headingCount = countHeadings(content);
        double avgSentenceLength = sentenceCount > 0 ? (double) wordCount / sentenceCount : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wordCount", wordCount);
        result.put("sentenceCount", sentenceCount);
        result.put("paragraphCount", paragraphCount);
        result.put("headingCount", headingCount);
        result.put("avgSentenceLength", BigDecimal.valueOf(avgSentenceLength).setScale(1, RoundingMode.HALF_UP));

        return toJson(result);
    }

    /**
     * Calculates readability score (0-100).
     *
     * @param content the text content to analyze
     * @return readability score
     */
    public BigDecimal calculateReadabilityScore(String content) {
        int wordCount = countWords(content);
        int sentenceCount = countSentences(content);
        int paragraphCount = countParagraphs(content);
        int headingCount = countHeadings(content);
        double avgSentenceLength = sentenceCount > 0 ? (double) wordCount / sentenceCount : 0;

        BigDecimal score = BigDecimal.valueOf(100);

        // Average sentence length scoring (max 40 points deduction)
        // Optimal: 20-30 characters/words per sentence
        if (avgSentenceLength < 10) {
            score = score.subtract(BigDecimal.valueOf(20)); // Too short sentences
        } else if (avgSentenceLength > 40) {
            score = score.subtract(BigDecimal.valueOf(40)); // Very long sentences
        } else if (avgSentenceLength > 30) {
            score = score.subtract(BigDecimal.valueOf(20)); // Somewhat long
        }
        // 20-30 is optimal, no deduction

        // Paragraph count scoring (max 20 points deduction)
        if (paragraphCount < 3) {
            score = score.subtract(BigDecimal.valueOf(20)); // Too few paragraphs
        } else if (paragraphCount < 5 && wordCount > 500) {
            score = score.subtract(BigDecimal.valueOf(10)); // Could use more paragraphs
        }

        // Heading usage scoring (max 20 points deduction)
        if (headingCount == 0 && wordCount > 200) {
            score = score.subtract(BigDecimal.valueOf(20)); // No headings in long content
        } else if (headingCount < 2 && wordCount > 500) {
            score = score.subtract(BigDecimal.valueOf(10)); // Few headings for length
        }

        // Sentence variety bonus (max 10 points deduction)
        if (sentenceCount < 3 && wordCount > 100) {
            score = score.subtract(BigDecimal.valueOf(10)); // Too few sentences
        }

        // Clamp between 0 and 100
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String cleaned = content.replaceAll("#+ ", "").replaceAll("<[^>]+>", "");
        return cleaned.trim().split("\\s+").length;
    }

    private int countSentences(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String cleaned = content.replaceAll("#+ ", "").replaceAll("<[^>]+>", "");
        // Split on sentence-ending punctuation
        String[] sentences = cleaned.split("[.!?]+");
        int count = 0;
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    private int countParagraphs(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String[] paragraphs = content.split("\n\\s*\n");
        int count = 0;
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    private int countHeadings(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        // Count Markdown headings (## or ###)
        Pattern markdownPattern = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE);
        Matcher markdownMatcher = markdownPattern.matcher(content);
        int count = 0;
        while (markdownMatcher.find()) {
            count++;
        }

        // Count HTML headings
        Pattern htmlPattern = Pattern.compile("<h[1-6][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher htmlMatcher = htmlPattern.matcher(content);
        while (htmlMatcher.find()) {
            count++;
        }

        return count;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize readability result", e);
            return "{}";
        }
    }
}
