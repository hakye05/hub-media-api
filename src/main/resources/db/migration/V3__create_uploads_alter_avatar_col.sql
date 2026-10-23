ALTER TABLE users RENAME COLUMN avatar_url TO avatar_key;
ALTER TABLE users ALTER COLUMN avatar_key TYPE VARCHAR(255);

CREATE TABLE uploads (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind         VARCHAR(20) NOT NULL,
    image_type   VARCHAR(10) NOT NULL,
    size_bytes   BIGINT      NOT NULL CHECK (size_bytes > 0),
    status       VARCHAR(20) NOT NULL,
    media_key    VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_uploads_user_status ON uploads (user_id, status);
CREATE INDEX idx_uploads_pending_expiry ON uploads (expires_at) WHERE status = 'PENDING';