-- Content Analyses table
CREATE TABLE content_analyses (
    id                          BIGSERIAL       PRIMARY KEY,
    site_id                     BIGINT          REFERENCES sites(id) ON DELETE SET NULL,
    user_id                     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title                       VARCHAR(500),
    content                     TEXT            NOT NULL,
    target_keywords             VARCHAR(1000),
    seo_score                   NUMERIC(5, 2),
    readability_score           NUMERIC(5, 2),
    keyword_density             JSONB,
    structure_analysis          JSONB,
    suggestions                 JSONB,
    generated_meta_title        VARCHAR(200),
    generated_meta_description  VARCHAR(500),
    ai_provider                 VARCHAR(20),
    ai_model                    VARCHAR(50),
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    error_message               VARCHAR(2000),
    completed_at                TIMESTAMP,
    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_analyses_user_id ON content_analyses (user_id);
CREATE INDEX idx_content_analyses_user_status ON content_analyses (user_id, status);
CREATE INDEX idx_content_analyses_user_site ON content_analyses (user_id, site_id);
CREATE INDEX idx_content_analyses_created_at ON content_analyses (created_at DESC);
