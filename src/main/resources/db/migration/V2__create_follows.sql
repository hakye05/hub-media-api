-- ---------------------------------------------------------------------------
-- Schema for user following
-- ---------------------------------------------------------------------------
CREATE TABLE follows (
    follower_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);

CREATE INDEX idx_follows_followee ON follows (followee_id, created_at DESC);
CREATE INDEX idx_follows_follower ON follows (follower_id, created_at DESC);