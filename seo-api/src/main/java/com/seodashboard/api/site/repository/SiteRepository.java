package com.seodashboard.api.site.repository;

import com.seodashboard.common.domain.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    Page<Site> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Optional<Site> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndUrl(Long userId, String url);
}
