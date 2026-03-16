package com.seodashboard.api.publicseo.repository;

import com.seodashboard.common.domain.PublicAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicAnalysisRepository extends JpaRepository<PublicAnalysis, Long> {

    List<PublicAnalysis> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<PublicAnalysis> findByDomainOrderByCreatedAtDesc(String domain, Pageable pageable);

    List<PublicAnalysis> findByStatusOrderBySeoScoreDesc(String status, Pageable pageable);

    List<PublicAnalysis> findTop20ByOrderByCreatedAtDesc();
}
