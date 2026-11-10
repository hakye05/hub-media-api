CREATE TABLE post_colors (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id  UUID             NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    position INTEGER          NOT NULL,
    hex      VARCHAR(7)       NOT NULL,
    lab_l    DOUBLE PRECISION NOT NULL,
    lab_a    DOUBLE PRECISION NOT NULL,
    lab_b    DOUBLE PRECISION NOT NULL,
    CONSTRAINT uq_post_colors_position UNIQUE (post_id, position)
);

CREATE INDEX idx_post_colors_post ON post_colors (post_id);