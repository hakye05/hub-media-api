CREATE TABLE collections (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(100) NOT NULL,
    is_public  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_collections_id_user UNIQUE (id, user_id)
);

CREATE UNIQUE INDEX uq_collections_user_title ON collections (user_id, lower(title));
CREATE INDEX idx_collections_user_feed ON collections (user_id, created_at DESC, id DESC);

CREATE TABLE collection_posts (
    collection_id UUID        NOT NULL,
    user_id       UUID        NOT NULL,
    post_id       UUID        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (collection_id, post_id),
    FOREIGN KEY (collection_id, user_id) REFERENCES collections (id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id, post_id)       REFERENCES saved_posts (user_id, post_id) ON DELETE CASCADE
);

CREATE INDEX idx_collection_posts_feed  ON collection_posts (collection_id, created_at DESC, post_id DESC);
CREATE INDEX idx_collection_posts_saved ON collection_posts (user_id, post_id);