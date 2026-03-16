package com.seodashboard.crawler.analyzer;

import com.seodashboard.crawler.dto.SeoIssue;
import com.seodashboard.crawler.engine.HtmlParser;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MetaTagAnalyzer {

    public MetaTagResult analyze(HtmlParser.ParseResult parseResult) {
        List<SeoIssue> issues = new ArrayList<>();

        BigDecimal titleScore = analyzeTitleScore(parseResult.getTitle(), issues);
        BigDecimal metaDescriptionScore = analyzeMetaDescriptionScore(parseResult.getMetaDescription(), issues);

        if (!parseResult.isHasOgTags()) {
            issues.add(SeoIssue.warning("MISSING_OG_TAGS", "Open Graph tags are missing"));
        }

        if (!parseResult.isHasTwitterCards()) {
            issues.add(SeoIssue.info("MISSING_TWITTER_CARDS", "Twitter Card tags are missing"));
        }

        if (parseResult.getCanonicalUrl() == null || parseResult.getCanonicalUrl().isBlank()) {
            issues.add(SeoIssue.warning("MISSING_CANONICAL", "Canonical URL is not specified"));
        }

        return new MetaTagResult(
                titleScore,
                parseResult.getTitle() != null ? parseResult.getTitle().length() : 0,
                metaDescriptionScore,
                parseResult.getMetaDescription() != null ? parseResult.getMetaDescription().length() : 0,
                issues
        );
    }

    private BigDecimal analyzeTitleScore(String title, List<SeoIssue> issues) {
        if (title == null || title.isBlank()) {
            issues.add(SeoIssue.error("MISSING_TITLE", "Page title is missing"));
            return BigDecimal.ZERO;
        }

        int length = title.length();
        double score = 100.0;

        if (length < 30) {
            score = 60.0;
            issues.add(SeoIssue.warning("TITLE_TOO_SHORT",
                    "Title is too short (" + length + " chars). Recommended: 30-60 characters",
                    Map.of("length", length, "recommended_min", 30, "recommended_max", 60)));
        } else if (length > 60) {
            score = 70.0;
            issues.add(SeoIssue.warning("TITLE_TOO_LONG",
                    "Title is too long (" + length + " chars). Recommended: 30-60 characters",
                    Map.of("length", length, "recommended_min", 30, "recommended_max", 60)));
        }

        return BigDecimal.valueOf(score);
    }

    private BigDecimal analyzeMetaDescriptionScore(String description, List<SeoIssue> issues) {
        if (description == null || description.isBlank()) {
            issues.add(SeoIssue.error("MISSING_META_DESC", "Meta description is missing"));
            return BigDecimal.ZERO;
        }

        int length = description.length();
        double score = 100.0;

        if (length < 120) {
            score = 60.0;
            issues.add(SeoIssue.warning("META_DESC_TOO_SHORT",
                    "Meta description is too short (" + length + " chars). Recommended: 120-160 characters",
                    Map.of("length", length, "recommended_min", 120, "recommended_max", 160)));
        } else if (length > 160) {
            score = 70.0;
            issues.add(SeoIssue.warning("META_DESC_TOO_LONG",
                    "Meta description is too long (" + length + " chars). Recommended: 120-160 characters",
                    Map.of("length", length, "recommended_min", 120, "recommended_max", 160)));
        }

        return BigDecimal.valueOf(score);
    }

    public record MetaTagResult(
            BigDecimal titleScore,
            int titleLength,
            BigDecimal metaDescriptionScore,
            int metaDescriptionLength,
            List<SeoIssue> issues
    ) {}
}
