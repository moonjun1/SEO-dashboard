-- Keywords table
CREATE TABLE keywords (
    id                  BIGSERIAL       PRIMARY KEY,
    site_id             BIGINT          NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    keyword             VARCHAR(500)    NOT NULL,
    target_url          VARCHAR(2048),
    search_engine       VARCHAR(20)     NOT NULL DEFAULT 'GOOGLE',
    country_code        VARCHAR(10)     NOT NULL DEFAULT 'KR',
    language_code       VARCHAR(10)     NOT NULL DEFAULT 'ko',
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

ALTER TABLE keywords ADD CONSTRAINT uk_keyword_site_engine_country
    UNIQUE (site_id, keyword, search_engine, country_code);

CREATE INDEX idx_keywords_site_id ON keywords (site_id);
CREATE INDEX idx_keywords_site_id_active ON keywords (site_id, is_active);

-- Keyword Rankings table
CREATE TABLE keyword_rankings (
    id                  BIGSERIAL       PRIMARY KEY,
    keyword_id          BIGINT          NOT NULL REFERENCES keywords(id) ON DELETE CASCADE,
    recorded_at         TIMESTAMP       NOT NULL,
    rank                INTEGER,
    url                 VARCHAR(2048),
    search_volume       INTEGER,
    previous_rank       INTEGER,
    rank_change         INTEGER
);

CREATE INDEX idx_keyword_rankings_keyword_id ON keyword_rankings (keyword_id);
CREATE INDEX idx_keyword_rankings_keyword_recorded ON keyword_rankings (keyword_id, recorded_at DESC);
