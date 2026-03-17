package com.seodashboard.crawler.analyzer;

import com.seodashboard.crawler.engine.HtmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetaTagAnalyzer")
class MetaTagAnalyzerTest {

    private MetaTagAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new MetaTagAnalyzer();
    }

    // ------------------------------------------------------------------
    // Helper: build a minimal ParseResult with sensible defaults so each
    // test only needs to override the field it cares about.
    // ------------------------------------------------------------------
    private HtmlParser.ParseResult.ParseResultBuilder baseParseResult() {
        return HtmlParser.ParseResult.builder()
                .title("A reasonable title that is between thirty and sixty chars")
                .metaDescription("A reasonable meta description that is between one-hundred-twenty and one-sixty characters long enough here.")
                .canonicalUrl("https://example.com/page")
                .hasOgTags(true)
                .hasTwitterCards(true)
                .hasStructuredData(false)
                .isMobileFriendly(true)
                .headings(List.of())
                .imagesTotal(0)
                .imagesWithoutAlt(0)
                .internalLinks(List.of())
                .externalLinks(List.of());
    }

    @Nested
    @DisplayName("Title scoring")
    class TitleScoring {

        @Test
        @DisplayName("null title returns score 0 and an ERROR issue")
        void nullTitle_returnsZeroScoreAndError() {
            HtmlParser.ParseResult pr = baseParseResult().title(null).build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.titleScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.titleLength()).isZero();
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_TITLE".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("blank title returns score 0 and an ERROR issue")
        void blankTitle_returnsZeroScoreAndError() {
            HtmlParser.ParseResult pr = baseParseResult().title("   ").build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.titleScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_TITLE".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("title shorter than 30 chars returns score 60 and a WARNING issue")
        void shortTitle_returnsSixtyScoreAndWarning() {
            HtmlParser.ParseResult pr = baseParseResult().title("Too short").build(); // 9 chars

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.titleScore()).isEqualByComparingTo(BigDecimal.valueOf(60));
            assertThat(result.issues())
                    .anyMatch(i -> "TITLE_TOO_SHORT".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("title longer than 60 chars returns score 70 and a WARNING issue")
        void longTitle_returnsSeventyScoreAndWarning() {
            String longTitle = "This title is deliberately made longer than sixty characters to trigger the warning";
            HtmlParser.ParseResult pr = baseParseResult().title(longTitle).build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.titleScore()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(result.issues())
                    .anyMatch(i -> "TITLE_TOO_LONG".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("title in optimal 30-60 char range returns score 100 with no title issue")
        void optimalTitle_returnsHundredScore() {
            String optimalTitle = "Optimal title right between thirty and sixty"; // 44 chars
            HtmlParser.ParseResult pr = baseParseResult().title(optimalTitle).build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.titleScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues())
                    .noneMatch(i -> i.type().startsWith("TITLE_"));
        }

        @Test
        @DisplayName("title length of exactly 30 chars is optimal (boundary)")
        void titleAtLowerBoundary_returnsHundredScore() {
            String title = "123456789012345678901234567890"; // exactly 30
            HtmlParser.ParseResult pr = baseParseResult().title(title).build();

            assertThat(analyzer.analyze(pr).titleScore())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("title length of exactly 60 chars is optimal (boundary)")
        void titleAtUpperBoundary_returnsHundredScore() {
            String title = "A".repeat(60);
            HtmlParser.ParseResult pr = baseParseResult().title(title).build();

            assertThat(analyzer.analyze(pr).titleScore())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    @Nested
    @DisplayName("Meta description scoring")
    class MetaDescriptionScoring {

        @Test
        @DisplayName("null meta description returns score 0 and an ERROR issue")
        void nullDescription_returnsZeroScoreAndError() {
            HtmlParser.ParseResult pr = baseParseResult().metaDescription(null).build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.metaDescriptionScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.metaDescriptionLength()).isZero();
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_META_DESC".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("blank meta description returns score 0 and an ERROR issue")
        void blankDescription_returnsZeroScoreAndError() {
            HtmlParser.ParseResult pr = baseParseResult().metaDescription("  ").build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.metaDescriptionScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_META_DESC".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("meta description shorter than 120 chars returns score 60 and a WARNING issue")
        void shortDescription_returnsSixtyScoreAndWarning() {
            HtmlParser.ParseResult pr = baseParseResult().metaDescription("Too short").build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.metaDescriptionScore()).isEqualByComparingTo(BigDecimal.valueOf(60));
            assertThat(result.issues())
                    .anyMatch(i -> "META_DESC_TOO_SHORT".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("meta description longer than 160 chars returns score 70 and a WARNING issue")
        void longDescription_returnsSeventyScoreAndWarning() {
            String longDesc = "A".repeat(161);
            HtmlParser.ParseResult pr = baseParseResult().metaDescription(longDesc).build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.metaDescriptionScore()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(result.issues())
                    .anyMatch(i -> "META_DESC_TOO_LONG".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("meta description in 120-160 char range returns score 100 with no desc issue")
        void optimalDescription_returnsHundredScore() {
            String desc = "A".repeat(140);
            HtmlParser.ParseResult pr = baseParseResult().metaDescription(desc).build();

            MetaTagAnalyzer.MetaTagResult result = analyzer.analyze(pr);

            assertThat(result.metaDescriptionScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues())
                    .noneMatch(i -> i.type().startsWith("META_DESC_"));
        }
    }

    @Nested
    @DisplayName("OG / Twitter / canonical detection")
    class SocialAndCanonical {

        @Test
        @DisplayName("missing OG tags produces a WARNING issue")
        void missingOgTags_producesWarning() {
            HtmlParser.ParseResult pr = baseParseResult().hasOgTags(false).build();

            assertThat(analyzer.analyze(pr).issues())
                    .anyMatch(i -> "MISSING_OG_TAGS".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("present OG tags produce no OG issue")
        void presentOgTags_noOgIssue() {
            HtmlParser.ParseResult pr = baseParseResult().hasOgTags(true).build();

            assertThat(analyzer.analyze(pr).issues())
                    .noneMatch(i -> "MISSING_OG_TAGS".equals(i.type()));
        }

        @Test
        @DisplayName("missing Twitter cards produces an INFO issue")
        void missingTwitterCards_producesInfo() {
            HtmlParser.ParseResult pr = baseParseResult().hasTwitterCards(false).build();

            assertThat(analyzer.analyze(pr).issues())
                    .anyMatch(i -> "MISSING_TWITTER_CARDS".equals(i.type()) && "INFO".equals(i.severity()));
        }

        @Test
        @DisplayName("present Twitter cards produce no Twitter issue")
        void presentTwitterCards_noTwitterIssue() {
            HtmlParser.ParseResult pr = baseParseResult().hasTwitterCards(true).build();

            assertThat(analyzer.analyze(pr).issues())
                    .noneMatch(i -> "MISSING_TWITTER_CARDS".equals(i.type()));
        }

        @Test
        @DisplayName("null canonical URL produces a WARNING issue")
        void nullCanonical_producesWarning() {
            HtmlParser.ParseResult pr = baseParseResult().canonicalUrl(null).build();

            assertThat(analyzer.analyze(pr).issues())
                    .anyMatch(i -> "MISSING_CANONICAL".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("blank canonical URL produces a WARNING issue")
        void blankCanonical_producesWarning() {
            HtmlParser.ParseResult pr = baseParseResult().canonicalUrl("").build();

            assertThat(analyzer.analyze(pr).issues())
                    .anyMatch(i -> "MISSING_CANONICAL".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("present canonical URL produces no canonical issue")
        void presentCanonical_noCanonicalIssue() {
            HtmlParser.ParseResult pr = baseParseResult().canonicalUrl("https://example.com/page").build();

            assertThat(analyzer.analyze(pr).issues())
                    .noneMatch(i -> "MISSING_CANONICAL".equals(i.type()));
        }
    }
}
