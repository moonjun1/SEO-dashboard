package com.seodashboard.crawler.analyzer;

import com.seodashboard.crawler.dto.SeoIssue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ImageAnalyzer {

    public ImageResult analyze(int imagesTotal, int imagesWithoutAlt) {
        List<SeoIssue> issues = new ArrayList<>();

        if (imagesTotal == 0) {
            return new ImageResult(BigDecimal.valueOf(100), issues);
        }

        double altRatio = (double) (imagesTotal - imagesWithoutAlt) / imagesTotal;
        double score = altRatio * 100.0;

        if (imagesWithoutAlt > 0) {
            issues.add(SeoIssue.warning("MISSING_ALT",
                    imagesWithoutAlt + " of " + imagesTotal + " images are missing alt attributes",
                    Map.of("imagesWithoutAlt", imagesWithoutAlt, "imagesTotal", imagesTotal)));
        }

        return new ImageResult(BigDecimal.valueOf(Math.round(score)), issues);
    }

    public record ImageResult(
            BigDecimal imageScore,
            List<SeoIssue> issues
    ) {}
}
