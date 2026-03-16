package com.seodashboard.api.keyword.repository;

import com.seodashboard.common.domain.KeywordRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KeywordRankingRepository extends JpaRepository<KeywordRanking, Long> {

    List<KeywordRanking> findByKeywordIdAndRecordedAtAfterOrderByRecordedAtDesc(
            Long keywordId, LocalDateTime after);

    Optional<KeywordRanking> findTopByKeywordIdOrderByRecordedAtDesc(Long keywordId);

    List<KeywordRanking> findByKeywordIdOrderByRecordedAtDesc(Long keywordId, Pageable pageable);
}
