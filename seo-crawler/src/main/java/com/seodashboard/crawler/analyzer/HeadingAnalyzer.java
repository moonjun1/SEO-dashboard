package com.seodashboard.crawler.analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.crawler.dto.SeoIssue;
import com.seodashboard.crawler.engine.HtmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeadingAnalyzer {

    private final ObjectMapper objectMapper;

    public HeadingResult analyze(List<HtmlParser.HeadingInfo> headings) {
        List<SeoIssue> issues = new ArrayList<>();
        double score = 100.0;

        long h1Count = headings.stream().filter(h -> "h1".equals(h.level())).count();
        long h2Count = headings.stream().filter(h -> "h2".equals(h.level())).count();

        if (h1Count == 0) {
            score -= 40;
            issues.add(SeoIssue.error("MISSING_H1", "Page is missing an H1 heading"));
        } else if (h1Count > 1) {
            score -= 20;
            issues.add(SeoIssue.warning("MULTIPLE_H1",
                    "Page has multiple H1 headings (" + h1Count + "). Recommended: exactly 1",
                    Map.of("count", h1Count)));
        }

        if (h2Count == 0) {
            score -= 20;
            issues.add(SeoIssue.warning("MISSING_H2", "Page has no H2 headings for content structure"));
        }

        // Check heading hierarchy (no skipping levels)
        if (!headings.isEmpty()) {
            boolean hasHierarchyIssue = false;
            int previousLevel = 0;
            for (HtmlParser.HeadingInfo heading : headings) {
                int currentLevel = Integer.parseInt(heading.level().substring(1));
                if (previousLevel > 0 && currentLevel > previousLevel + 1) {
                    hasHierarchyIssue = true;
                }
                previousLevel = currentLevel;
            }
            if (hasHierarchyIssue) {
                score -= 10;
                issues.add(SeoIssue.warning("HEADING_HIERARCHY_SKIP",
                        "Heading hierarchy skips levels (e.g., H1 to H3 without H2)"));
            }
        }

        score = Math.max(0, score);

        String headingStructure = buildHeadingStructureJson(headings);

        return new HeadingResult(BigDecimal.valueOf(score), headingStructure, issues);
    }

    private String buildHeadingStructureJson(List<HtmlParser.HeadingInfo> headings) {
        List<Map<String, String>> structure = new ArrayList<>();
        for (HtmlParser.HeadingInfo heading : headings) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("level", heading.level());
            item.put("text", heading.text().length() > 100
                    ? heading.text().substring(0, 100) + "..."
                    : heading.text());
            structure.add(item);
        }
        try {
            return objectMapper.writeValueAsString(structure);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize heading structure: {}", e.getMessage());
            return "[]";
        }
    }

    public record HeadingResult(
            BigDecimal headingScore,
            String headingStructure,
            List<SeoIssue> issues
    ) {}
}
