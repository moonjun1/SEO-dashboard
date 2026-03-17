package com.seodashboard.crawler.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LinkAnalyzer")
class LinkAnalyzerTest {

    private LinkAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new LinkAnalyzer();
    }

    @Nested
    @DisplayName("Internal-links-only scenarios")
    class InternalLinksOnly {

        @Test
        @DisplayName("internal links with no broken links returns score 100 and no issues")
        void internalLinksNoBroken_returnsPerfectScore() {
            LinkAnalyzer.LinkResult result = analyzer.analyze(5, 0, 0);

            assertThat(result.linkScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("External links present")
    class ExternalLinks {

        @Test
        @DisplayName("internal and external links with no broken links returns score 100")
        void internalAndExternal_noBroken_returnsPerfectScore() {
            LinkAnalyzer.LinkResult result = analyzer.analyze(3, 2, 0);

            assertThat(result.linkScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues()).isEmpty();
        }

        @Test
        @DisplayName("external-only links with no internal links deducts 30 points")
        void externalOnlyNoInternal_deductsThirtyPoints() {
            LinkAnalyzer.LinkResult result = analyzer.analyze(0, 5, 0);

            // 100 - 30 = 70
            assertThat(result.linkScore()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(result.issues())
                    .anyMatch(i -> "NO_INTERNAL_LINKS".equals(i.type()) && "WARNING".equals(i.severity()));
        }
    }

    @Nested
    @DisplayName("No links at all")
    class NoLinks {

        @Test
        @DisplayName("zero links deducts 30 points for no internal links")
        void noLinks_deductsThirtyPoints() {
            LinkAnalyzer.LinkResult result = analyzer.analyze(0, 0, 0);

            // 100 - 30 (no internal) = 70
            assertThat(result.linkScore()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(result.issues())
                    .anyMatch(i -> "NO_INTERNAL_LINKS".equals(i.type()));
        }
    }

    @Nested
    @DisplayName("Broken links")
    class BrokenLinks {

        @Test
        @DisplayName("single broken link among ten total deducts 10 points")
        void oneBrokenLinkOutOfTen_deductsTenPoints() {
            // internal=5, external=5, broken=1 → total=10
            // brokenRatio = 1/10 = 0.1 → penalty = min(50, 10) = 10
            LinkAnalyzer.LinkResult result = analyzer.analyze(5, 5, 1);

            assertThat(result.linkScore()).isEqualByComparingTo(BigDecimal.valueOf(90));
            assertThat(result.issues())
                    .anyMatch(i -> "BROKEN_LINK".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("all links broken caps deduction at 50 points")
        void allLinksBroken_cappedAtFiftyPointDeduction() {
            // internal=2, external=2, broken=4 → brokenRatio=1.0 → penalty capped at 50
            LinkAnalyzer.LinkResult result = analyzer.analyze(2, 2, 4);

            assertThat(result.linkScore()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(result.issues())
                    .anyMatch(i -> "BROKEN_LINK".equals(i.type()));
        }

        @Test
        @DisplayName("score never goes below 0 even with no internal links and many broken links")
        void combinedPenalties_scoreNotNegative() {
            // 100 - 30 (no internal) - 50 (cap) = 20, not negative
            LinkAnalyzer.LinkResult result = analyzer.analyze(0, 2, 2);

            assertThat(result.linkScore().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("broken link issue message contains broken count and total")
        void brokenLinkIssue_containsCountsInMessage() {
            LinkAnalyzer.LinkResult result = analyzer.analyze(4, 0, 2);

            assertThat(result.issues())
                    .anyMatch(i -> "BROKEN_LINK".equals(i.type())
                            && i.message().contains("2"));
        }
    }
}
