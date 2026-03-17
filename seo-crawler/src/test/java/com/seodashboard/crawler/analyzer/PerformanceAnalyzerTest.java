package com.seodashboard.crawler.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerformanceAnalyzer")
class PerformanceAnalyzerTest {

    private PerformanceAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PerformanceAnalyzer();
    }

    @Nested
    @DisplayName("Fast responses (< 500 ms)")
    class FastResponses {

        @ParameterizedTest(name = "{0} ms")
        @ValueSource(ints = {0, 100, 300, 499})
        @DisplayName("response < 500 ms returns score 100 with no issues")
        void fastResponse_returnsHundredScore(int responseTimeMs) {
            PerformanceAnalyzer.PerformanceResult result = analyzer.analyze(responseTimeMs);

            assertThat(result.performanceScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Acceptable responses (500 – 999 ms)")
    class AcceptableResponses {

        @ParameterizedTest(name = "{0} ms")
        @ValueSource(ints = {500, 750, 999})
        @DisplayName("response 500-999 ms returns score 80 with no issues")
        void mediumResponse_returnsEightyScore(int responseTimeMs) {
            PerformanceAnalyzer.PerformanceResult result = analyzer.analyze(responseTimeMs);

            assertThat(result.performanceScore()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Slow responses (1000 – 1999 ms)")
    class SlowResponses {

        @ParameterizedTest(name = "{0} ms")
        @ValueSource(ints = {1000, 1500, 1999})
        @DisplayName("response 1000-1999 ms returns score 60 with no issues")
        void slowResponse_returnsSixtyScore(int responseTimeMs) {
            PerformanceAnalyzer.PerformanceResult result = analyzer.analyze(responseTimeMs);

            assertThat(result.performanceScore()).isEqualByComparingTo(BigDecimal.valueOf(60));
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Very slow responses (2000 – 2999 ms)")
    class VerySlowResponses {

        @ParameterizedTest(name = "{0} ms")
        @ValueSource(ints = {2000, 2500, 2999})
        @DisplayName("response 2000-2999 ms returns score 40 with a WARNING")
        void verySlowResponse_returnsFortyScoreWithWarning(int responseTimeMs) {
            PerformanceAnalyzer.PerformanceResult result = analyzer.analyze(responseTimeMs);

            assertThat(result.performanceScore()).isEqualByComparingTo(BigDecimal.valueOf(40));
            assertThat(result.issues())
                    .hasSize(1)
                    .anyMatch(i -> "SLOW_RESPONSE".equals(i.type()) && "WARNING".equals(i.severity()));
        }
    }

    @Nested
    @DisplayName("Critically slow responses (>= 3000 ms)")
    class CriticallySlowResponses {

        @ParameterizedTest(name = "{0} ms")
        @ValueSource(ints = {3000, 5000, 10000})
        @DisplayName("response >= 3000 ms returns score 20 with an ERROR")
        void criticallySlowResponse_returnsTwentyScoreWithError(int responseTimeMs) {
            PerformanceAnalyzer.PerformanceResult result = analyzer.analyze(responseTimeMs);

            assertThat(result.performanceScore()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(result.issues())
                    .hasSize(1)
                    .anyMatch(i -> "SLOW_RESPONSE".equals(i.type()) && "ERROR".equals(i.severity()));
        }

        @Test
        @DisplayName("slow response issue message contains the actual response time")
        void slowResponseIssue_containsActualResponseTimeInMessage() {
            PerformanceAnalyzer.PerformanceResult result = analyzer.analyze(3500);

            assertThat(result.issues())
                    .anyMatch(i -> "SLOW_RESPONSE".equals(i.type())
                            && i.message().contains("3500"));
        }
    }
}
