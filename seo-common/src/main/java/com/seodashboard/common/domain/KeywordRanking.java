package com.seodashboard.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_rankings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "url", length = 2048)
    private String url;

    @Column(name = "search_volume")
    private Integer searchVolume;

    @Column(name = "previous_rank")
    private Integer previousRank;

    @Column(name = "rank_change")
    private Integer rankChange;

    @Builder
    public KeywordRanking(Keyword keyword, LocalDateTime recordedAt, Integer rank,
                          String url, Integer searchVolume, Integer previousRank, Integer rankChange) {
        this.keyword = keyword;
        this.recordedAt = recordedAt;
        this.rank = rank;
        this.url = url;
        this.searchVolume = searchVolume;
        this.previousRank = previousRank;
        this.rankChange = rankChange;
    }
}
