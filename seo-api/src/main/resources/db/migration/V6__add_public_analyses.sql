-- Public SEO analysis table (no authentication required)
CREATE TABLE public_analyses (
    id                      BIGSERIAL       PRIMARY KEY,
    url                     VARCHAR(2048)   NOT NULL,
    domain                  VARCHAR(500)    NOT NULL,
    seo_score               NUMERIC(5, 2),
    title_score             NUMERIC(5, 2),
    meta_description_score  NUMERIC(5, 2),
    heading_score           NUMERIC(5, 2),
    image_score             NUMERIC(5, 2),
    link_score              NUMERIC(5, 2),
    performance_score       NUMERIC(5, 2),
    title                   VARCHAR(1000),
    meta_description        TEXT,
    canonical_url           VARCHAR(2048),
    response_time_ms        INTEGER,
    content_length          INTEGER,
    total_images            INTEGER         NOT NULL DEFAULT 0,
    images_without_alt      INTEGER         NOT NULL DEFAULT 0,
    internal_links          INTEGER         NOT NULL DEFAULT 0,
    external_links          INTEGER         NOT NULL DEFAULT 0,
    broken_links            INTEGER         NOT NULL DEFAULT 0,
    total_headings          INTEGER         NOT NULL DEFAULT 0,
    has_og_tags             BOOLEAN         NOT NULL DEFAULT FALSE,
    has_twitter_cards       BOOLEAN         NOT NULL DEFAULT FALSE,
    has_viewport            BOOLEAN         NOT NULL DEFAULT FALSE,
    has_favicon             BOOLEAN         NOT NULL DEFAULT FALSE,
    has_robots_txt          BOOLEAN         NOT NULL DEFAULT FALSE,
    has_sitemap             BOOLEAN         NOT NULL DEFAULT FALSE,
    has_https               BOOLEAN         NOT NULL DEFAULT FALSE,
    heading_structure       JSONB,
    issues                  JSONB,
    link_list               JSONB,
    meta_tags               JSONB,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    error_message           VARCHAR(2000),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_public_analyses_domain ON public_analyses (domain);
CREATE INDEX idx_public_analyses_seo_score ON public_analyses (seo_score DESC);
CREATE INDEX idx_public_analyses_created_at ON public_analyses (created_at DESC);
CREATE INDEX idx_public_analyses_status ON public_analyses (status);
