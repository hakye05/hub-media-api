-- ---------------------------------------------------------------------------
-- Schema for saved collections
-- ---------------------------------------------------------------------------
CREATE TABLE saved_collections (
    user_id       UUID        NOT NULL REFERENCES users (id)       ON DELETE CASCADE,
    collection_id UUID        NOT NULL REFERENCES collections (id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, collection_id)
);

CREATE INDEX idx_saved_collections_user_feed  ON saved_collections (user_id, created_at DESC, collection_id DESC);
CREATE INDEX idx_saved_collections_collection ON saved_collections (collection_id);