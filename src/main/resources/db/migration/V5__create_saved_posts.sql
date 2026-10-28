-- ---------------------------------------------------------------------------
-- Schema for saved posts
-- ---------------------------------------------------------------------------
CREATE TABLE saved_posts (
    user_id  UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id  UUID           NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

CREATE INDEX idx_saved_posts_user_feed ON saved_posts (user_id, created_at DESC, post_id DESC);
CREATE INDEX idx_saved_posts_post ON saved_posts (post_id);