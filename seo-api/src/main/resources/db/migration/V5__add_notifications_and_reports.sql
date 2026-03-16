-- Notifications table
CREATE TABLE notifications (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                VARCHAR(50)     NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    message             TEXT,
    reference_type      VARCHAR(50),
    reference_id        BIGINT,
    severity            VARCHAR(20)     NOT NULL DEFAULT 'INFO',
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_type ON notifications (user_id, type);

-- Reports table
CREATE TABLE reports (
    id                  BIGSERIAL       PRIMARY KEY,
    site_id             BIGINT          NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                VARCHAR(20)     NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,
    summary             JSONB,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_site_id ON reports (site_id);
CREATE INDEX idx_reports_user_id ON reports (user_id);
CREATE INDEX idx_reports_site_created ON reports (site_id, created_at DESC);
CREATE INDEX idx_reports_status ON reports (status);
