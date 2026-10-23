CREATE TABLE posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_key   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_posts_image_key UNIQUE (image_key)
);

CREATE INDEX idx_posts_feed      ON posts (created_at DESC, id DESC);
CREATE INDEX idx_posts_user_feed ON posts (user_id, created_at DESC, id DESC);