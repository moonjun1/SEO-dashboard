-- Crawl Jobs table
CREATE TABLE crawl_jobs (
    id                  BIGSERIAL       PRIMARY KEY,
    site_id             BIGINT          NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    trigger_type        VARCHAR(20)     NOT NULL,
    max_pages           INTEGER         NOT NULL DEFAULT 100,
    max_depth           INTEGER         NOT NULL DEFAULT 3,
    total_pages         INTEGER,
    error_count         INTEGER         NOT NULL DEFAULT 0,
    error_message       VARCHAR(2000),
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crawl_jobs_site_id ON crawl_jobs (site_id);
CREATE INDEX idx_crawl_jobs_site_id_status ON crawl_jobs (site_id, status);
CREATE INDEX idx_crawl_jobs_created_at ON crawl_jobs (created_at DESC);

-- Crawl Results table
CREATE TABLE crawl_results (
    id                  BIGSERIAL       PRIMARY KEY,
    crawl_job_id        BIGINT          NOT NULL REFERENCES crawl_jobs(id) ON DELETE CASCADE,
    url                 VARCHAR(2048)   NOT NULL,
    status_code         INTEGER,
    content_type        VARCHAR(255),
    content_length      BIGINT,
    response_time_ms    INTEGER,
    title               VARCHAR(1000),
    meta_description    TEXT,
    canonical_url       VARCHAR(2048),
    depth               INTEGER         NOT NULL DEFAULT 0,
    is_internal         BOOLEAN         NOT NULL DEFAULT TRUE,
    redirect_url        VARCHAR(2048),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crawl_results_crawl_job_id ON crawl_results (crawl_job_id);
CREATE INDEX idx_crawl_results_url ON crawl_results (url);

-- Page Analyses table
CREATE TABLE page_analyses (
    id                      BIGSERIAL       PRIMARY KEY,
    crawl_result_id         BIGINT          NOT NULL UNIQUE REFERENCES crawl_results(id) ON DELETE CASCADE,
    seo_score               NUMERIC(5, 2),
    title_score             NUMERIC(5, 2),
    title_length            INTEGER,
    meta_description_score  NUMERIC(5, 2),
    meta_description_length INTEGER,
    heading_score           NUMERIC(5, 2),
    heading_structure       JSONB,
    image_score             NUMERIC(5, 2),
    images_total            INTEGER         NOT NULL DEFAULT 0,
    images_without_alt      INTEGER         NOT NULL DEFAULT 0,
    link_score              NUMERIC(5, 2),
    internal_links_count    INTEGER         NOT NULL DEFAULT 0,
    external_links_count    INTEGER         NOT NULL DEFAULT 0,
    broken_links_count      INTEGER         NOT NULL DEFAULT 0,
    performance_score       NUMERIC(5, 2),
    has_og_tags             BOOLEAN,
    has_twitter_cards       BOOLEAN,
    has_structured_data     BOOLEAN,
    has_sitemap             BOOLEAN,
    has_robots_txt          BOOLEAN,
    is_mobile_friendly      BOOLEAN,
    issues                  JSONB,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_page_analyses_crawl_result_id ON page_analyses (crawl_result_id);
CREATE INDEX idx_page_analyses_seo_score ON page_analyses (seo_score);
