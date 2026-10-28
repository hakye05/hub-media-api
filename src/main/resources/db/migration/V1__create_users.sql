-- ---------------------------------------------------------------------------
-- Schema for users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    age           INTEGER      NOT NULL,
    gender        VARCHAR(20)  NOT NULL,
    bio           VARCHAR(500),
    avatar_key    VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email        UNIQUE (email),
    CONSTRAINT uq_users_username     UNIQUE (username),
    CONSTRAINT chk_users_email_lower CHECK (email = lower(email)),
    CONSTRAINT chk_users_username_lower CHECK (username = lower(username)),
    CONSTRAINT chk_users_age         CHECK (age BETWEEN 13 AND 120)
);