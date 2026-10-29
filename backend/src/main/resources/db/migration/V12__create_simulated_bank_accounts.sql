-- V12__create_simulated_bank_accounts.sql
-- Simulated funding sources per user. No real bank integration.
-- simulated_balance >= 0 enforced by CHECK.
-- version column for JPA optimistic locking.
-- Also adds the deferred FK from transactions.simulated_account_id
-- (could not be added in V4 because this table did not exist yet).

CREATE TABLE simulated_bank_accounts (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    bank_name           VARCHAR(100)    NOT NULL,
    account_nickname    VARCHAR(100)    NOT NULL,
    account_identifier  VARCHAR(50)     NOT NULL,
    simulated_balance   NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_simulated_bank_accounts_identifier UNIQUE (account_identifier),
    CONSTRAINT chk_simulated_bank_accounts_balance   CHECK  (simulated_balance >= 0)
);

CREATE INDEX idx_sba_user_id           ON simulated_bank_accounts (user_id);
CREATE UNIQUE INDEX idx_sba_identifier ON simulated_bank_accounts (account_identifier);

-- Add the FK that was deferred from V4 to avoid a forward reference.
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_simulated_account
    FOREIGN KEY (simulated_account_id)
    REFERENCES simulated_bank_accounts (id)
    ON DELETE SET NULL;

CREATE INDEX idx_transactions_simulated_account_id ON transactions (simulated_account_id);