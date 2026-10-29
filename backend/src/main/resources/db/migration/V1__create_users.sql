-- V1__create_users.sql
-- Users table: authentication, roles, account lock state.

CREATE TABLE users (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL,
    email          VARCHAR(255)   NOT NULL,
    phone_number   VARCHAR(20)    NOT NULL,
    password_hash  VARCHAR(255)   NOT NULL,
    pin_hash       VARCHAR(255)   NOT NULL,
    role           VARCHAR(10)    NOT NULL DEFAULT 'USER',
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN        NOT NULL DEFAULT FALSE,
    locked_until   TIMESTAMP      NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email        UNIQUE (email),
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number),
    CONSTRAINT chk_users_role        CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_email        ON users (email);
CREATE INDEX idx_users_phone_number ON users (phone_number);
CREATE INDEX idx_users_account_locked ON users (account_locked) WHERE account_locked = TRUE;