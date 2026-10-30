-- V1__create_users.sql
-- Users table: authentication, roles, account lock state.
-- CORRECTED: Uses correct column names (password, transaction_pin, enabled)
-- matching the User JPA entity mappings.

CREATE TABLE users (
    id                BIGSERIAL      PRIMARY KEY,
    name              VARCHAR(100)   NOT NULL,
    email             VARCHAR(255)   NOT NULL UNIQUE,
    phone_number      VARCHAR(20)    NOT NULL UNIQUE,
    password          VARCHAR(255)   NOT NULL,
    transaction_pin   VARCHAR(255)   NOT NULL,
    role              VARCHAR(10)    NOT NULL DEFAULT 'USER',
    enabled           BOOLEAN        NOT NULL DEFAULT TRUE,
    account_locked    BOOLEAN        NOT NULL DEFAULT FALSE,
    locked_until      TIMESTAMP      NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone_number ON users (phone_number);
CREATE INDEX idx_users_account_locked ON users (account_locked) WHERE account_locked = TRUE;
