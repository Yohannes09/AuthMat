CREATE SEQUENCE IF NOT EXISTS user_id_sequence
    START WITH 11957103
    INCREMENT BY 9;

CREATE TABLE IF NOT EXISTS users (
    id                      BIGINT          PRIMARY KEY DEFAULT nextval('user_id_sequence'),
    external_id             VARCHAR         NOT NULL UNIQUE,
    username                VARCHAR(50)     NOT NULL UNIQUE,
    hashed_password         VARCHAR(255),
    email                   VARCHAR(255)    UNIQUE,
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    account_non_expired     BOOLEAN         NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN         NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN         NOT NULL DEFAULT TRUE,
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT,
    provider                VARCHAR(50),
    provider_id             VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_users_external_id ON users (external_id);
CREATE INDEX IF NOT EXISTS idx_users_email       ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_provider    ON users (provider, provider_id);