package com.seodashboard.crawler.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.crawler.engine.HtmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("SeoAnalyzer")
class SeoAnalyzerTest {

    private SeoAnalyzer analyzer;

    // Weights declared in SeoAnalyzer
    private static final double TITLE_WEIGHT       = 0.20;
    private static final double META_DESC_WEIGHT   = 0.15;
    private static final double HEADING_WEIGHT     = 0.15;
    private static final double IMAGE_WEIGHT       = 0.15;
    private static final double LINK_WEIGHT        = 0.15;
    private static final double PERFORMANCE_WEIGHT = 0.20;

    @BeforeEach
    void setUp() {
        analyzer = new SeoAnalyzer(
                new MetaTagAnalyzer(),
                new HeadingAnalyzer(new ObjectMapper()),
                new ImageAnalyzer(),
                new LinkAnalyzer(),
                new PerformanceAnalyzer()
        );
    }

    /**
     * A ParseResult that would yield a perfect 100 score from every sub-analyzer:
     * - title 30-60 chars
     * - meta description 120-160 chars
     * - exactly one H1, at least one H2, proper hierarchy
     * - no images (perfect score by definition)
     * - internal links present, no broken links
     * - response time < 500 ms (passed separately)
     */
    private HtmlParser.ParseResult perfectParseResult() {
        return HtmlParser.ParseResult.builder()
                .title("Optimal title between thirty and sixty chars here") // 50 chars
                .metaDescription("A".repeat(140))   // 140 chars → 100 score
                .canonicalUrl("https://example.com/page")
                .hasOgTags(true)
                .hasTwitterCards(true)
                .hasStructuredData(false)
                .isMobileFriendly(true)
                .headings(List.of(
                        new HtmlParser.HeadingInfo("h1", "Main heading"),
                        new HtmlParser.HeadingInfo("h2", "Section")))
                .imagesTotal(0)
                .imagesWithoutAlt(0)
                .internalLinks(List.of("https://example.com/about"))
                .externalLinks(List.of())
                .build();
    }

    @Nested
    @DisplayName("Weighted average calculation")
    class WeightedAverage {

        @Test
        @DisplayName("perfect inputs across all sub-analyzers yields total score 100")
        void perfectInputs_yieldsHundredScore() {
            SeoAnalyzer.AnalysisResult result = analyzer.analyze(perfectParseResult(), 200, 0);

            assertThat(result.getSeoScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("manually computed weighted score matches analyzer output")
        void weightedScoreFormula_matchesOutput() {
            // Deliberately degraded inputs so each component has a known score:
            // title: null → 0
            // metaDesc: null → 0
            // heading: no H1, no H2 → 40
            // images: none → 100
            // links: internal only, no broken → 100
            // performance: 200ms → 100
            HtmlParser.ParseResult pr = HtmlParser.ParseResult.builder()
                    .title(null)
                    .metaDescription(null)
                    .canonicalUrl(null)
                    .hasOgTags(false)
                    .hasTwitterCards(false)
                    .hasStructuredData(false)
                    .isMobileFriendly(false)
                    .headings(List.of())          // 40 score
                    .imagesTotal(0)
                    .imagesWithoutAlt(0)
                    .internalLinks(List.of("https://example.com/about"))
                    .externalLinks(List.of())
                    .build();

            double expectedScore =
                    0   * TITLE_WEIGHT       // title
                  + 0   * META_DESC_WEIGHT   // metaDescription
                  + 40  * HEADING_WEIGHT     // headings
                  + 100 * IMAGE_WEIGHT       // images
                  + 100 * LINK_WEIGHT        // links
                  + 100 * PERFORMANCE_WEIGHT;// performance (200ms)

            SeoAnalyzer.AnalysisResult result = analyzer.analyze(pr, 200, 0);

            assertThat(result.getSeoScore().doubleValue())
                    .isCloseTo(expectedScore, within(0.01));
        }

        @Test
        @DisplayName("degraded performance score is reflected in weighted total")
        void degradedPerformance_lowersTotalScore() {
            // perfect parse result but very slow response
            SeoAnalyzer.AnalysisResult fast   = analyzer.analyze(perfectParseResult(), 200,   0);
            SeoAnalyzer.AnalysisResult slow   = analyzer.analyze(perfectParseResult(), 5000,  0);

            assertThat(fast.getSeoScore()).isGreaterThan(slow.getSeoScore());
        }
    }

    @Nested
    @DisplayName("AnalysisResult field population")
    class FieldPopulation {

        @Test
        @DisplayName("result exposes all sub-analyzer scores")
        void result_exposesAllScores() {
            SeoAnalyzer.AnalysisResult result = analyzer.analyze(perfectParseResult(), 200, 0);

            assertThat(result.getTitleScore()).isNotNull();
            assertThat(result.getMetaDescriptionScore()).isNotNull();
            assertThat(result.getHeadingScore()).isNotNull();
            assertThat(result.getImageScore()).isNotNull();
            assertThat(result.getLinkScore()).isNotNull();
            assertThat(result.getPerformanceScore()).isNotNull();
        }

        @Test
        @DisplayName("result title length is populated from parse result")
        void result_titleLengthPopulated() {
            HtmlParser.ParseResult pr = perfectParseResult();
            SeoAnalyzer.AnalysisResult result = analyzer.analyze(pr, 200, 0);

            assertThat(result.getTitleLength()).isEqualTo(pr.getTitle().length());
        }

        @Test
        @DisplayName("result meta description length is populated from parse result")
        void result_metaDescriptionLengthPopulated() {
            HtmlParser.ParseResult pr = perfectParseResult();
            SeoAnalyzer.AnalysisResult result = analyzer.analyze(pr, 200, 0);

            assertThat(result.getMetaDescriptionLength()).isEqualTo(pr.getMetaDescription().length());
        }

        @Test
        @DisplayName("result boolean flags mirror parse result flags")
        void result_booleanFlagsMirrorParseResult() {
            HtmlParser.ParseResult pr = perfectParseResult();
            SeoAnalyzer.AnalysisResult result = analyzer.analyze(pr, 200, 0);

            assertThat(result.getHasOgTags()).isTrue();
            assertThat(result.getHasTwitterCards()).isTrue();
            assertThat(result.getIsMobileFriendly()).isTrue();
        }

        @Test
        @DisplayName("broken links count in result matches the value passed in")
        void result_brokenLinksCountMatchesInput() {
            SeoAnalyzer.AnalysisResult result = analyzer.analyze(perfectParseResult(), 200, 3);

            assertThat(result.getBrokenLinksCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("aggregate issues list is non-null and contains issues from all failing sub-analyzers")
        void result_issuesListAggregatesAllSubAnalyzers() {
            // title missing (ERROR), metaDesc missing (ERROR), no OG (WARNING), no Twitter (INFO),
            // no canonical (WARNING), no H1 (ERROR), no H2 (WARNING)
            HtmlParser.ParseResult pr = HtmlParser.ParseResult.builder()
                    .title(null)
                    .metaDescription(null)
                    .canonicalUrl(null)
                    .hasOgTags(false)
                    .hasTwitterCards(false)
                    .hasStructuredData(false)
                    .isMobileFriendly(false)
                    .headings(List.of())
                    .imagesTotal(0)
                    .imagesWithoutAlt(0)
                    .internalLinks(List.of())
                    .externalLinks(List.of())
                    .build();

            SeoAnalyzer.AnalysisResult result = analyzer.analyze(pr, 200, 0);

            assertThat(result.getIssues()).isNotNull().isNotEmpty();
            // Spot-check a few expected issue types
            assertThat(result.getIssues())
                    .anyMatch(i -> "MISSING_TITLE".equals(i.type()))
                    .anyMatch(i -> "MISSING_META_DESC".equals(i.type()))
                    .anyMatch(i -> "MISSING_H1".equals(i.type()))
                    .anyMatch(i -> "MISSING_OG_TAGS".equals(i.type()))
                    .anyMatch(i -> "NO_INTERNAL_LINKS".equals(i.type()));
        }
    }
}
