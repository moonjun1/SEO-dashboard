package com.seodashboard.crawler.analyzer;

import com.seodashboard.crawler.dto.SeoIssue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LinkAnalyzer {

    public LinkResult analyze(int internalLinksCount, int externalLinksCount, int brokenLinksCount) {
        List<SeoIssue> issues = new ArrayList<>();
        double score = 100.0;

        int totalLinks = internalLinksCount + externalLinksCount;

        if (internalLinksCount == 0) {
            score -= 30;
            issues.add(SeoIssue.warning("NO_INTERNAL_LINKS", "Page has no internal links"));
        }

        if (brokenLinksCount > 0) {
            double brokenRatio = totalLinks > 0 ? (double) brokenLinksCount / totalLinks : 0;
            score -= Math.min(50, brokenRatio * 100);
            issues.add(SeoIssue.error("BROKEN_LINK",
                    brokenLinksCount + " broken link(s) detected on this page",
                    Map.of("brokenLinksCount", brokenLinksCount, "totalLinks", totalLinks)));
        }

        score = Math.max(0, score);

        return new LinkResult(BigDecimal.valueOf(Math.round(score)), issues);
    }

    public record LinkResult(
            BigDecimal linkScore,
            List<SeoIssue> issues
    ) {}
}
