-- ---------------------------------------------------------------------------
-- Schema for user following
-- ---------------------------------------------------------------------------
CREATE TABLE follows (
    follower_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> followee_id)
);

-- Keyset pagination of followers / following (newest first); the PK covers "does A follow B".
CREATE INDEX idx_follows_followee_cursor ON follows (followee_id, created_at DESC, follower_id DESC);
CREATE INDEX idx_follows_follower_cursor ON follows (follower_id, created_at DESC, followee_id DESC);