-- Users table
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_is_active ON users (is_active);

-- Sites table
CREATE TABLE sites (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    url                 VARCHAR(2048)   NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(1000),
    seo_score           NUMERIC(5, 2),
    last_crawled_at     TIMESTAMP,
    crawl_interval_hours INTEGER        NOT NULL DEFAULT 168,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sites_user_id ON sites (user_id);
CREATE INDEX idx_sites_user_id_is_active ON sites (user_id, is_active);
CREATE UNIQUE INDEX idx_sites_user_id_url ON sites (user_id, url);
