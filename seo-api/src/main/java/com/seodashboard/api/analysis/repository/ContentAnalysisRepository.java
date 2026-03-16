package com.seodashboard.api.analysis.repository;

import com.seodashboard.ai.service.ContentAnalysisExecutor;
import com.seodashboard.common.domain.ContentAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, Long>,
        ContentAnalysisExecutor.ContentAnalysisJpaRepository {

    Page<ContentAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ContentAnalysis> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    Page<ContentAnalysis> findByUserIdAndSiteId(Long userId, Long siteId, Pageable pageable);
}
