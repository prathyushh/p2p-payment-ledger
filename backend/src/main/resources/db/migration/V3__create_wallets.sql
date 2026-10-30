-- V3__create_wallets.sql
-- One wallet per user. balance >= 0 and daily_spent >= 0 enforced by CHECK.
-- CORRECTED: Includes daily_limit column matching the Wallet JPA entity.
-- version column used for JPA optimistic locking.

CREATE TABLE wallets (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    balance     NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    daily_spent NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    daily_limit NUMERIC(15, 2)  NOT NULL DEFAULT 50000.00,
    status      VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_wallets_balance     CHECK (balance >= 0),
    CONSTRAINT chk_wallets_daily_spent CHECK (daily_spent >= 0),
    CONSTRAINT chk_wallets_status      CHECK (status IN ('ACTIVE', 'INACTIVE', 'FROZEN'))
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_status ON wallets (status);
