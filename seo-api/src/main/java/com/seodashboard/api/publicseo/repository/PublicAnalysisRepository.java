package com.seodashboard.api.publicseo.repository;

import com.seodashboard.common.domain.PublicAnalysis;
import com.seodashboard.common.domain.enums.AnalysisStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicAnalysisRepository extends JpaRepository<PublicAnalysis, Long> {

    List<PublicAnalysis> findByStatusOrderByCreatedAtDesc(AnalysisStatus status, Pageable pageable);

    List<PublicAnalysis> findByDomainOrderByCreatedAtDesc(String domain, Pageable pageable);

    List<PublicAnalysis> findByStatusOrderBySeoScoreDesc(AnalysisStatus status, Pageable pageable);

    List<PublicAnalysis> findTop20ByOrderByCreatedAtDesc();
}
