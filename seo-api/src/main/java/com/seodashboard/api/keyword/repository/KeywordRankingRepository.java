package com.seodashboard.api.keyword.repository;

import com.seodashboard.common.domain.KeywordRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KeywordRankingRepository extends JpaRepository<KeywordRanking, Long> {

    List<KeywordRanking> findByKeywordIdAndRecordedAtAfterOrderByRecordedAtDesc(
            Long keywordId, LocalDateTime after);

    Optional<KeywordRanking> findTopByKeywordIdOrderByRecordedAtDesc(Long keywordId);

    List<KeywordRanking> findByKeywordIdOrderByRecordedAtDesc(Long keywordId, Pageable pageable);

    /**
     * Fetch the latest ranking per keyword in a single query.
     * Uses a correlated subquery to find the max recordedAt per keyword_id.
     */
    @Query("SELECT kr FROM KeywordRanking kr WHERE kr.keyword.id IN :keywordIds " +
           "AND kr.recordedAt = (SELECT MAX(kr2.recordedAt) FROM KeywordRanking kr2 WHERE kr2.keyword.id = kr.keyword.id)")
    List<KeywordRanking> findLatestByKeywordIdIn(@Param("keywordIds") List<Long> keywordIds);
}
