-- V4__create_transactions.sql
-- All wallet transactions: P2P_TRANSFER, WALLET_TOP_UP, AUTO_RELOAD.
-- request_id enforces client-side idempotency.
-- simulated_account_id FK added in V12 (forward reference avoided).
-- Note: sender_wallet_id and receiver_wallet_id are both NULLABLE:
--   P2P_TRANSFER sender: sender_wallet_id set, receiver_wallet_id set after settlement
--   WALLET_TOP_UP / AUTO_RELOAD: sender_wallet_id NULL, receiver_wallet_id = wallet being credited

CREATE TABLE transactions (
    id                   BIGSERIAL       PRIMARY KEY,
    request_id           VARCHAR(100)    NOT NULL,
    type                 VARCHAR(20)     NOT NULL,
    sender_wallet_id     BIGINT          NULL REFERENCES wallets (id) ON DELETE SET NULL,
    receiver_wallet_id   BIGINT          NULL REFERENCES wallets (id) ON DELETE SET NULL,
    amount               NUMERIC(15, 2)  NOT NULL,
    status               VARCHAR(10)     NOT NULL,
    remark               VARCHAR(255)    NULL,
    settlement_due_date  DATE            NULL,
    simulated_account_id BIGINT          NULL,
    initiated_at         TIMESTAMP       NOT NULL DEFAULT NOW(),
    settled_at           TIMESTAMP       NULL,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transactions_request_id UNIQUE (request_id),
    CONSTRAINT chk_transactions_type      CHECK (type   IN ('P2P_TRANSFER', 'WALLET_TOP_UP', 'AUTO_RELOAD')),
    CONSTRAINT chk_transactions_status    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'DECLINED')),
    CONSTRAINT chk_transactions_amount    CHECK (amount > 0)
);

CREATE INDEX idx_transactions_request_id        ON transactions (request_id);
CREATE INDEX idx_transactions_sender_wallet_id   ON transactions (sender_wallet_id);
CREATE INDEX idx_transactions_receiver_wallet_id ON transactions (receiver_wallet_id);
CREATE INDEX idx_transactions_status             ON transactions (status);
CREATE INDEX idx_transactions_settlement_due     ON transactions (settlement_due_date) WHERE status = 'PENDING';
CREATE INDEX idx_transactions_initiated_at       ON transactions (initiated_at DESC);