-- V5__create_ledger_entries.sql
-- Immutable double-entry ledger. One DEBIT on transfer initiation;
-- one CREDIT on settlement. balance_before + amount = balance_after
-- is enforced at the application layer.

CREATE TABLE ledger_entries (
    id              BIGSERIAL       PRIMARY KEY,
    transaction_id  BIGINT          NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    wallet_id       BIGINT          NOT NULL REFERENCES wallets (id)      ON DELETE CASCADE,
    type            VARCHAR(6)      NOT NULL,
    amount          NUMERIC(15, 2)  NOT NULL,
    balance_before  NUMERIC(15, 2)  NOT NULL,
    balance_after   NUMERIC(15, 2)  NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ledger_type   CHECK (type   IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount CHECK (amount > 0)
);

CREATE INDEX idx_ledger_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_wallet_id      ON ledger_entries (wallet_id);
CREATE INDEX idx_ledger_created_at     ON ledger_entries (created_at DESC);