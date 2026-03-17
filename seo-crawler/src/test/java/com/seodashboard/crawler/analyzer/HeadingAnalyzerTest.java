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

@DisplayName("HeadingAnalyzer")
class HeadingAnalyzerTest {

    private HeadingAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new HeadingAnalyzer(new ObjectMapper());
    }

    private static HtmlParser.HeadingInfo h(String level, String text) {
        return new HtmlParser.HeadingInfo(level, text);
    }

    @Nested
    @DisplayName("No headings at all")
    class NoHeadings {

        @Test
        @DisplayName("empty heading list returns score 40 with both H1 and H2 errors")
        void emptyList_returnsFortyScore() {
            HeadingAnalyzer.HeadingResult result = analyzer.analyze(List.of());

            // 100 - 40 (no H1) - 20 (no H2) = 40
            assertThat(result.headingScore()).isEqualByComparingTo(BigDecimal.valueOf(40));
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_H1".equals(i.type()) && "ERROR".equals(i.severity()))
                    .anyMatch(i -> "MISSING_H2".equals(i.type()) && "WARNING".equals(i.severity()));
        }
    }

    @Nested
    @DisplayName("H1 rules")
    class H1Rules {

        @Test
        @DisplayName("single H1 with H2 present produces no H1 issue")
        void oneH1_noH1Issue() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h1", "Main heading"),
                    h("h2", "Section")
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            assertThat(result.issues())
                    .noneMatch(i -> "MISSING_H1".equals(i.type()))
                    .noneMatch(i -> "MULTIPLE_H1".equals(i.type()));
        }

        @Test
        @DisplayName("no H1 deducts 40 points and raises an ERROR")
        void noH1_deductsFourtyPoints() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h2", "Section"),
                    h("h3", "Sub-section")
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            // 100 - 40 (no H1) = 60  (H2 is present so no H2 deduction)
            assertThat(result.headingScore()).isEqualByComparingTo(BigDecimal.valueOf(60));
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_H1".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("two H1 headings deduct 20 points and raise a WARNING")
        void twoH1_deductsTwentyPoints() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h1", "First"),
                    h("h1", "Second"),
                    h("h2", "Section")
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            // 100 - 20 (multiple H1) = 80
            assertThat(result.headingScore()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(result.issues())
                    .anyMatch(i -> "MULTIPLE_H1".equals(i.type()) && "WARNING".equals(i.severity()));
        }
    }

    @Nested
    @DisplayName("H2 rules")
    class H2Rules {

        @Test
        @DisplayName("no H2 deducts 20 points and raises a WARNING")
        void noH2_deductsTwentyPoints() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h1", "Main heading")
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            // 100 - 20 (no H2) = 80
            assertThat(result.headingScore()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_H2".equals(i.type()) && "WARNING".equals(i.severity()));
        }
    }

    @Nested
    @DisplayName("Heading hierarchy")
    class HierarchyRules {

        @Test
        @DisplayName("H1 -> H3 (skipping H2) deducts 10 points and raises a WARNING")
        void h1ToH3Skip_deductsTenPoints() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h1", "Main"),
                    h("h3", "Sub-sub section") // skips h2
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            // 100 - 20 (no H2 counted) - 10 (hierarchy skip) = 70
            assertThat(result.issues())
                    .anyMatch(i -> "HEADING_HIERARCHY_SKIP".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("H1 -> H2 -> H3 proper hierarchy produces score 100 with no issues")
        void properHierarchy_returnsHundredScore() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h1", "Main heading"),
                    h("h2", "Section"),
                    h("h3", "Sub-section")
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            assertThat(result.headingScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues()).isEmpty();
        }

        @Test
        @DisplayName("H2 -> H4 skip (without any H1) raises hierarchy WARNING in addition to H1 error")
        void h2ToH4Skip_raisesHierarchyWarning() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h2", "Section"),
                    h("h4", "Deep sub")   // skips h3
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            assertThat(result.issues())
                    .anyMatch(i -> "HEADING_HIERARCHY_SKIP".equals(i.type()));
        }
    }

    @Nested
    @DisplayName("headingStructure JSON")
    class HeadingStructureJson {

        @Test
        @DisplayName("heading structure is serialized as a JSON array")
        void headingStructure_isJsonArray() {
            List<HtmlParser.HeadingInfo> headings = List.of(
                    h("h1", "Title"),
                    h("h2", "Section")
            );

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            assertThat(result.headingStructure()).startsWith("[").endsWith("]");
        }

        @Test
        @DisplayName("empty headings serializes to '[]'")
        void emptyHeadings_serializedAsEmptyArray() {
            HeadingAnalyzer.HeadingResult result = analyzer.analyze(List.of());

            assertThat(result.headingStructure()).isEqualTo("[]");
        }

        @Test
        @DisplayName("heading text longer than 100 chars is truncated with ellipsis")
        void longHeadingText_isTruncated() {
            String longText = "A".repeat(150);
            List<HtmlParser.HeadingInfo> headings = List.of(h("h1", longText), h("h2", "Section"));

            HeadingAnalyzer.HeadingResult result = analyzer.analyze(headings);

            assertThat(result.headingStructure()).contains("...");
        }
    }
}
