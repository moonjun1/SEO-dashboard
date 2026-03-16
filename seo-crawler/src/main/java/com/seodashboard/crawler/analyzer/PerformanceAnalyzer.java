package com.seodashboard.crawler.analyzer;

import com.seodashboard.crawler.dto.SeoIssue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PerformanceAnalyzer {

    public PerformanceResult analyze(int responseTimeMs) {
        List<SeoIssue> issues = new ArrayList<>();

        double score;
        if (responseTimeMs < 500) {
            score = 100;
        } else if (responseTimeMs < 1000) {
            score = 80;
        } else if (responseTimeMs < 2000) {
            score = 60;
        } else if (responseTimeMs < 3000) {
            score = 40;
            issues.add(SeoIssue.warning("SLOW_RESPONSE",
                    "Page response time is slow (" + responseTimeMs + "ms). Target: under 1000ms",
                    Map.of("responseTimeMs", responseTimeMs)));
        } else {
            score = 20;
            issues.add(SeoIssue.error("SLOW_RESPONSE",
                    "Page response time is very slow (" + responseTimeMs + "ms). Target: under 1000ms",
                    Map.of("responseTimeMs", responseTimeMs)));
        }

        return new PerformanceResult(BigDecimal.valueOf(score), issues);
    }

    public record PerformanceResult(
            BigDecimal performanceScore,
            List<SeoIssue> issues
    ) {}
}
