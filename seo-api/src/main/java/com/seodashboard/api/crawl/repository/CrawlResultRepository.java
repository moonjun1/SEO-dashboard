package com.seodashboard.api.crawl.repository;

import com.seodashboard.common.domain.CrawlResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrawlResultRepository extends JpaRepository<CrawlResult, Long> {

    @Query("SELECT cr FROM CrawlResult cr LEFT JOIN PageAnalysis pa ON pa.crawlResult = cr " +
           "WHERE cr.crawlJob.id = :crawlJobId ORDER BY pa.seoScore ASC NULLS LAST")
    Page<CrawlResult> findByCrawlJobIdOrderBySeoScoreAsc(@Param("crawlJobId") Long crawlJobId, Pageable pageable);
}
