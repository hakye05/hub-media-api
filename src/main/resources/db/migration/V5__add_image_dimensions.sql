ALTER TABLE uploads
    ADD COLUMN width  INTEGER,
    ADD COLUMN height INTEGER;

ALTER TABLE posts
    ADD COLUMN width  INTEGER NOT NULL,
    ADD COLUMN height INTEGER NOT NULL,
    ADD CONSTRAINT chk_posts_dimensions CHECK (width > 0 AND height > 0);
