package com.seodashboard.api.crawl.repository;

import com.seodashboard.common.domain.CrawlJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {

    Page<CrawlJob> findBySiteIdOrderByCreatedAtDesc(Long siteId, Pageable pageable);

    boolean existsBySiteIdAndStatus(Long siteId, String status);
}
