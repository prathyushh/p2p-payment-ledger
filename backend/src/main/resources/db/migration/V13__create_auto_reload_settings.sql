-- V13__create_auto_reload_settings.sql
-- One Auto Reload configuration per wallet.
-- reload_in_progress is the concurrency guard (DEC-004): set to TRUE before
-- executing a reload, released in the application finally block.
-- threshold_amount and reload_amount must be > 0 when set.

CREATE TABLE auto_reload_settings (
    id                   BIGSERIAL       PRIMARY KEY,
    wallet_id            BIGINT          NOT NULL REFERENCES wallets (id)               ON DELETE CASCADE,
    is_enabled           BOOLEAN         NOT NULL DEFAULT FALSE,
    simulated_account_id BIGINT          NULL     REFERENCES simulated_bank_accounts (id) ON DELETE SET NULL,
    threshold_amount     NUMERIC(15, 2)  NULL,
    reload_amount        NUMERIC(15, 2)  NULL,
    reload_in_progress   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_auto_reload_wallet_id         UNIQUE  (wallet_id),
    CONSTRAINT chk_auto_reload_threshold_amount CHECK (threshold_amount IS NULL OR threshold_amount > 0),
    CONSTRAINT chk_auto_reload_reload_amount    CHECK (reload_amount    IS NULL OR reload_amount    > 0)
);

CREATE UNIQUE INDEX idx_auto_reload_wallet_id ON auto_reload_settings (wallet_id);
CREATE INDEX idx_auto_reload_simulated_account ON auto_reload_settings (simulated_account_id);