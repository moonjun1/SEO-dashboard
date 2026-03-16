package com.seodashboard.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "keywords", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_keyword_site_engine_country",
                columnNames = {"site_id", "keyword", "search_engine", "country_code"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false, length = 500)
    private String keyword;

    @Column(name = "target_url", length = 2048)
    private String targetUrl;

    @Column(name = "search_engine", nullable = false, length = 20)
    private String searchEngine = "GOOGLE";

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode = "KR";

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode = "ko";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public Keyword(Site site, String keyword, String targetUrl,
                   String searchEngine, String countryCode, String languageCode) {
        this.site = site;
        this.keyword = keyword;
        this.targetUrl = targetUrl;
        this.searchEngine = searchEngine != null ? searchEngine : "GOOGLE";
        this.countryCode = countryCode != null ? countryCode : "KR";
        this.languageCode = languageCode != null ? languageCode : "ko";
        this.isActive = true;
    }

    public void update(String targetUrl, Boolean isActive) {
        if (targetUrl != null) this.targetUrl = targetUrl;
        if (isActive != null) this.isActive = isActive;
    }
}
