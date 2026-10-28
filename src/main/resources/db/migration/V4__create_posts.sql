CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- Schema for user posts
-- ---------------------------------------------------------------------------
CREATE TABLE posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_key   VARCHAR(255) NOT NULL,
    width       INTEGER      NOT NULL,
    height      INTEGER      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_posts_image_key UNIQUE (image_key),
    CONSTRAINT chk_posts_dimensions CHECK (width > 0 AND height > 0)
);

-- Keyset pagination: global feed and per-user lists
CREATE INDEX idx_posts_feed      ON posts (created_at DESC, id DESC);
CREATE INDEX idx_posts_user_feed ON posts (user_id, created_at DESC, id DESC);

-- ILIKE '%term%' search on title / description
CREATE INDEX idx_posts_title_trgm       ON posts USING gin (title gin_trgm_ops);
CREATE INDEX idx_posts_description_trgm ON posts USING gin (description gin_trgm_ops);