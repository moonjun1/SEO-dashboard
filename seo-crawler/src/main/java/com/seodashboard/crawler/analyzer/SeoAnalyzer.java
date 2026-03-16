package com.seodashboard.crawler.analyzer;

import com.seodashboard.crawler.dto.SeoIssue;
import com.seodashboard.crawler.engine.HtmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoAnalyzer {

    private static final BigDecimal TITLE_WEIGHT = BigDecimal.valueOf(0.20);
    private static final BigDecimal META_DESC_WEIGHT = BigDecimal.valueOf(0.15);
    private static final BigDecimal HEADING_WEIGHT = BigDecimal.valueOf(0.15);
    private static final BigDecimal IMAGE_WEIGHT = BigDecimal.valueOf(0.15);
    private static final BigDecimal LINK_WEIGHT = BigDecimal.valueOf(0.15);
    private static final BigDecimal PERFORMANCE_WEIGHT = BigDecimal.valueOf(0.20);

    private final MetaTagAnalyzer metaTagAnalyzer;
    private final HeadingAnalyzer headingAnalyzer;
    private final ImageAnalyzer imageAnalyzer;
    private final LinkAnalyzer linkAnalyzer;
    private final PerformanceAnalyzer performanceAnalyzer;

    public AnalysisResult analyze(HtmlParser.ParseResult parseResult, int responseTimeMs,
                                   int brokenLinksCount) {
        List<SeoIssue> allIssues = new ArrayList<>();

        // Meta tags (title + description + OG + Twitter + canonical)
        MetaTagAnalyzer.MetaTagResult metaResult = metaTagAnalyzer.analyze(parseResult);
        allIssues.addAll(metaResult.issues());

        // Headings
        HeadingAnalyzer.HeadingResult headingResult = headingAnalyzer.analyze(parseResult.getHeadings());
        allIssues.addAll(headingResult.issues());

        // Images
        ImageAnalyzer.ImageResult imageResult = imageAnalyzer.analyze(
                parseResult.getImagesTotal(), parseResult.getImagesWithoutAlt());
        allIssues.addAll(imageResult.issues());

        // Links
        LinkAnalyzer.LinkResult linkResult = linkAnalyzer.analyze(
                parseResult.getInternalLinks().size(),
                parseResult.getExternalLinks().size(),
                brokenLinksCount);
        allIssues.addAll(linkResult.issues());

        // Performance
        PerformanceAnalyzer.PerformanceResult performanceResult = performanceAnalyzer.analyze(responseTimeMs);
        allIssues.addAll(performanceResult.issues());

        // Weighted total score
        BigDecimal seoScore = metaResult.titleScore().multiply(TITLE_WEIGHT)
                .add(metaResult.metaDescriptionScore().multiply(META_DESC_WEIGHT))
                .add(headingResult.headingScore().multiply(HEADING_WEIGHT))
                .add(imageResult.imageScore().multiply(IMAGE_WEIGHT))
                .add(linkResult.linkScore().multiply(LINK_WEIGHT))
                .add(performanceResult.performanceScore().multiply(PERFORMANCE_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);

        return AnalysisResult.builder()
                .seoScore(seoScore)
                .titleScore(metaResult.titleScore())
                .titleLength(metaResult.titleLength())
                .metaDescriptionScore(metaResult.metaDescriptionScore())
                .metaDescriptionLength(metaResult.metaDescriptionLength())
                .headingScore(headingResult.headingScore())
                .headingStructure(headingResult.headingStructure())
                .imageScore(imageResult.imageScore())
                .imagesTotal(parseResult.getImagesTotal())
                .imagesWithoutAlt(parseResult.getImagesWithoutAlt())
                .linkScore(linkResult.linkScore())
                .internalLinksCount(parseResult.getInternalLinks().size())
                .externalLinksCount(parseResult.getExternalLinks().size())
                .brokenLinksCount(brokenLinksCount)
                .performanceScore(performanceResult.performanceScore())
                .hasOgTags(parseResult.isHasOgTags())
                .hasTwitterCards(parseResult.isHasTwitterCards())
                .hasStructuredData(parseResult.isHasStructuredData())
                .isMobileFriendly(parseResult.isMobileFriendly())
                .issues(allIssues)
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class AnalysisResult {
        private final BigDecimal seoScore;
        private final BigDecimal titleScore;
        private final int titleLength;
        private final BigDecimal metaDescriptionScore;
        private final int metaDescriptionLength;
        private final BigDecimal headingScore;
        private final String headingStructure;
        private final BigDecimal imageScore;
        private final int imagesTotal;
        private final int imagesWithoutAlt;
        private final BigDecimal linkScore;
        private final int internalLinksCount;
        private final int externalLinksCount;
        private final int brokenLinksCount;
        private final BigDecimal performanceScore;
        private final Boolean hasOgTags;
        private final Boolean hasTwitterCards;
        private final Boolean hasStructuredData;
        private final Boolean isMobileFriendly;
        private final List<SeoIssue> issues;
    }
}
