-- ---------------------------------------------------------------------------
-- Initial schema for users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    age           INTEGER      NOT NULL CHECK (age BETWEEN 13 AND 120),
    gender        VARCHAR(20)  NOT NULL,
    bio           VARCHAR(500),
    avatar_url    VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_email    ON users (lower(email));
CREATE UNIQUE INDEX uq_users_username ON users (lower(username));