package com.seodashboard.crawler.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageAnalyzer")
class ImageAnalyzerTest {

    private ImageAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ImageAnalyzer();
    }

    @Nested
    @DisplayName("No images on the page")
    class NoImages {

        @Test
        @DisplayName("zero total images returns perfect score 100 with no issues")
        void noImages_returnsPerfectScore() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(0, 0);

            assertThat(result.imageScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("All images have alt text")
    class AllImagesWithAlt {

        @Test
        @DisplayName("all images have alt returns score 100 with no issues")
        void allWithAlt_returnsHundredScore() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(5, 0);

            assertThat(result.imageScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Some images missing alt text")
    class SomeImagesMissingAlt {

        @Test
        @DisplayName("half of images missing alt returns score 50 with a WARNING")
        void halfMissingAlt_returnsFiftyScore() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(4, 2);

            // (4-2)/4 * 100 = 50.0, rounded = 50
            assertThat(result.imageScore()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(result.issues())
                    .hasSize(1)
                    .anyMatch(i -> "MISSING_ALT".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("one of five images missing alt returns score 80 with a WARNING")
        void oneOfFiveMissingAlt_returnsEightyScore() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(5, 1);

            // (5-1)/5 * 100 = 80.0
            assertThat(result.imageScore()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_ALT".equals(i.type()));
        }

        @Test
        @DisplayName("issue message contains missing count and total")
        void issueMessage_containsCountsAndTotal() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(10, 3);

            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_ALT".equals(i.type())
                            && i.message().contains("3")
                            && i.message().contains("10"));
        }
    }

    @Nested
    @DisplayName("All images missing alt text")
    class AllImagesMissingAlt {

        @Test
        @DisplayName("all images missing alt returns score 0 with a WARNING")
        void allMissingAlt_returnsZeroScore() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(3, 3);

            assertThat(result.imageScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.issues())
                    .hasSize(1)
                    .anyMatch(i -> "MISSING_ALT".equals(i.type()) && "WARNING".equals(i.severity()));
        }

        @Test
        @DisplayName("single image missing alt returns score 0 with a WARNING")
        void singleImageMissingAlt_returnsZeroScore() {
            ImageAnalyzer.ImageResult result = analyzer.analyze(1, 1);

            assertThat(result.imageScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.issues())
                    .anyMatch(i -> "MISSING_ALT".equals(i.type()));
        }
    }
}
