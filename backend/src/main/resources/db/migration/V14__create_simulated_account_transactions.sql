-- V14__create_simulated_account_transactions.sql
-- Per-account audit trail for simulated balance changes (DEC-001, DEC-012).
-- Records DEMO_FUND_ADDED, WALLET_TOP_UP debit, and AUTO_RELOAD debit.
-- Demo fund additions appear here only; they do NOT appear in the main
-- transactions table (DEC-012, UQ-10).
-- related_transaction_id links to the wallet transaction where one exists.

CREATE TABLE simulated_account_transactions (
    id                     BIGSERIAL       PRIMARY KEY,
    simulated_account_id   BIGINT          NOT NULL REFERENCES simulated_bank_accounts (id) ON DELETE CASCADE,
    operation_type         VARCHAR(30)     NOT NULL,
    amount                 NUMERIC(15, 2)  NOT NULL,
    direction              VARCHAR(6)      NOT NULL,
    balance_before         NUMERIC(15, 2)  NOT NULL,
    balance_after          NUMERIC(15, 2)  NOT NULL,
    related_transaction_id BIGINT          NULL REFERENCES transactions (id) ON DELETE SET NULL,
    outcome                VARCHAR(10)     NOT NULL DEFAULT 'SUCCESS',
    notes                  VARCHAR(255)    NULL,
    created_at             TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sat_operation_type CHECK (operation_type IN ('DEMO_FUND_ADDED', 'WALLET_TOP_UP', 'AUTO_RELOAD')),
    CONSTRAINT chk_sat_direction       CHECK (direction       IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_sat_outcome         CHECK (outcome          IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_sat_amount          CHECK (amount > 0)
);

CREATE INDEX idx_sat_simulated_account_id   ON simulated_account_transactions (simulated_account_id);
CREATE INDEX idx_sat_created_at             ON simulated_account_transactions (created_at DESC);
CREATE INDEX idx_sat_related_transaction_id ON simulated_account_transactions (related_transaction_id);