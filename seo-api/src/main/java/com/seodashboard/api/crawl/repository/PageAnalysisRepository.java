package com.seodashboard.api.crawl.repository;

import com.seodashboard.common.domain.PageAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageAnalysisRepository extends JpaRepository<PageAnalysis, Long> {

    Optional<PageAnalysis> findByCrawlResultId(Long crawlResultId);

    List<PageAnalysis> findByCrawlResultIdIn(List<Long> crawlResultIds);
}
