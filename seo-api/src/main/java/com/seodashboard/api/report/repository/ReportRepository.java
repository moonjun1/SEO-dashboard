package com.seodashboard.api.report.repository;

import com.seodashboard.common.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findBySiteIdOrderByCreatedAtDesc(Long siteId, Pageable pageable);

    Optional<Report> findByIdAndSiteId(Long id, Long siteId);
}
