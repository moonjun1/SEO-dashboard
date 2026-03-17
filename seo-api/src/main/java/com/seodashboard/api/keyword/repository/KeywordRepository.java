package com.seodashboard.api.keyword.repository;

import com.seodashboard.common.domain.Keyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Page<Keyword> findBySiteIdAndIsActive(Long siteId, boolean isActive, Pageable pageable);

    Page<Keyword> findBySiteId(Long siteId, Pageable pageable);

    @Query("SELECT k FROM Keyword k WHERE k.site.id = :siteId AND LOWER(k.keyword) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Keyword> findBySiteIdAndKeywordContaining(@Param("siteId") Long siteId,
                                                    @Param("keyword") String keyword,
                                                    Pageable pageable);

    boolean existsBySiteIdAndKeywordAndSearchEngineAndCountryCode(
            Long siteId, String keyword, String searchEngine, String countryCode);

    List<Keyword> findBySiteIdAndIsActiveTrue(Long siteId);

    List<Keyword> findBySiteIdInAndIsActiveTrue(List<Long> siteIds);

    Optional<Keyword> findByIdAndSiteId(Long id, Long siteId);
}
