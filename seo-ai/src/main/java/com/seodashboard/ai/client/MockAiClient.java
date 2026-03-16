package com.seodashboard.ai.client;

import com.seodashboard.ai.dto.AiAnalysisResult;
import com.seodashboard.ai.dto.ContentSuggestion;
import com.seodashboard.ai.dto.MetaSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {

    private static final String PROVIDER = "MOCK";
    private static final String MODEL = "mock-v1";

    @Value("${ai.mock.delay-ms:2000}")
    private long delayMs;

    @Override
    public AiAnalysisResult analyzeContent(String content, List<String> targetKeywords) {
        log.info("MockAiClient: analyzing content (length={}, keywords={})", content.length(), targetKeywords);
        simulateDelay();

        List<ContentSuggestion> suggestions = new ArrayList<>();

        // Content length suggestions
        int wordCount = content.split("\\s+").length;
        if (wordCount < 300) {
            suggestions.add(new ContentSuggestion(
                    "CONTENT_LENGTH", "HIGH",
                    "Content is too short (" + wordCount + " words)",
                    "Aim for at least 300 words for better SEO performance. Consider adding more detailed explanations, examples, or related topics."
            ));
        } else if (wordCount > 3000) {
            suggestions.add(new ContentSuggestion(
                    "CONTENT_LENGTH", "MEDIUM",
                    "Content is very long (" + wordCount + " words)",
                    "Consider breaking this into multiple articles or adding a table of contents for better user experience."
            ));
        }

        // Heading structure suggestions
        if (!content.contains("##") && !content.contains("<h2") && !content.contains("<h3")) {
            suggestions.add(new ContentSuggestion(
                    "STRUCTURE", "HIGH",
                    "No subheadings detected",
                    "Add subheadings (H2, H3) to improve content structure and make it easier to scan."
            ));
        }

        // Keyword usage suggestions
        if (targetKeywords != null && !targetKeywords.isEmpty()) {
            String lowerContent = content.toLowerCase();
            for (String keyword : targetKeywords) {
                String lowerKeyword = keyword.toLowerCase().trim();
                if (!lowerKeyword.isEmpty() && !lowerContent.contains(lowerKeyword)) {
                    suggestions.add(new ContentSuggestion(
                            "KEYWORD_USAGE", "HIGH",
                            "Target keyword '" + keyword.trim() + "' not found in content",
                            "Include the keyword naturally in your content, especially in the first paragraph and headings."
                    ));
                }
            }

            // Check first paragraph keyword presence
            String firstParagraph = content.split("\n\n")[0].toLowerCase();
            for (String keyword : targetKeywords) {
                String lowerKeyword = keyword.toLowerCase().trim();
                if (!lowerKeyword.isEmpty() && lowerContent.contains(lowerKeyword) && !firstParagraph.contains(lowerKeyword)) {
                    suggestions.add(new ContentSuggestion(
                            "KEYWORD_PLACEMENT", "MEDIUM",
                            "Keyword '" + keyword.trim() + "' missing from first paragraph",
                            "Place important keywords in the first paragraph for better SEO relevance signals."
                    ));
                }
            }
        }

        // Paragraph length suggestions
        String[] paragraphs = content.split("\n\n");
        for (int i = 0; i < paragraphs.length; i++) {
            int paragraphWords = paragraphs[i].split("\\s+").length;
            if (paragraphWords > 150) {
                suggestions.add(new ContentSuggestion(
                        "READABILITY", "MEDIUM",
                        "Paragraph " + (i + 1) + " is too long (" + paragraphWords + " words)",
                        "Break long paragraphs into shorter ones (50-100 words) for better readability."
                ));
                break; // Only report once
            }
        }

        // Generate meta title and description from content
        String metaTitle = generateMetaTitle(content, targetKeywords);
        String metaDescription = generateMetaDescription(content, targetKeywords);

        return new AiAnalysisResult(suggestions, metaTitle, metaDescription, PROVIDER, MODEL);
    }

    @Override
    public List<MetaSuggestion> generateMeta(String content, List<String> targetKeywords, int count) {
        log.info("MockAiClient: generating {} meta suggestions", count);
        simulateDelay();

        List<MetaSuggestion> results = new ArrayList<>();
        String firstSentence = extractFirstSentence(content);
        String keywordPrefix = (targetKeywords != null && !targetKeywords.isEmpty())
                ? targetKeywords.getFirst().trim() + " - "
                : "";

        for (int i = 0; i < count; i++) {
            String title;
            String description;

            switch (i) {
                case 0 -> {
                    title = truncate(keywordPrefix + firstSentence, 60);
                    description = truncate(extractSummary(content), 160);
                }
                case 1 -> {
                    title = truncate("Guide: " + firstSentence, 60);
                    description = truncate("Learn about " + firstSentence.toLowerCase() + " " + extractSummary(content), 160);
                }
                default -> {
                    title = truncate(firstSentence + " | Complete Guide", 60);
                    description = truncate("Discover " + extractSummary(content).toLowerCase(), 160);
                }
            }

            results.add(new MetaSuggestion(title, title.length(), description, description.length()));
        }

        return results;
    }

    private String generateMetaTitle(String content, List<String> targetKeywords) {
        String firstSentence = extractFirstSentence(content);
        String keywordPrefix = (targetKeywords != null && !targetKeywords.isEmpty())
                ? targetKeywords.getFirst().trim() + " - "
                : "";
        return truncate(keywordPrefix + firstSentence, 60);
    }

    private String generateMetaDescription(String content, List<String> targetKeywords) {
        return truncate(extractSummary(content), 160);
    }

    private String extractFirstSentence(String content) {
        String cleaned = content.replaceAll("#+ ", "").replaceAll("<[^>]+>", "").trim();
        int dotIndex = cleaned.indexOf('.');
        if (dotIndex > 0 && dotIndex < 200) {
            return cleaned.substring(0, dotIndex + 1);
        }
        return truncate(cleaned, 100);
    }

    private String extractSummary(String content) {
        String cleaned = content.replaceAll("#+ ", "").replaceAll("<[^>]+>", "").trim();
        String[] sentences = cleaned.split("[.!?]");
        if (sentences.length >= 2) {
            return sentences[0].trim() + ". " + sentences[1].trim() + ".";
        }
        return truncate(cleaned, 160);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private void simulateDelay() {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
