-- V10__create_daily_summary.sql
-- One row per calendar day, created by the 11 PM reconciliation job.
-- reconciliation_mismatch = TRUE triggers a critical admin notification.

CREATE TABLE daily_summary (
    id                                BIGSERIAL       PRIMARY KEY,
    summary_date                      DATE            NOT NULL,
    total_transfers_initiated         INT             NOT NULL DEFAULT 0,
    total_transfers_initiated_amount  NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    total_transfers_settled           INT             NOT NULL DEFAULT 0,
    total_transfers_settled_amount    NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    fraud_alerts_raised               INT             NOT NULL DEFAULT 0,
    new_user_registrations            INT             NOT NULL DEFAULT 0,
    total_pending_settlement_amount   NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    reconciliation_debit_sum          NUMERIC(15, 2)  NULL,
    reconciliation_credit_sum         NUMERIC(15, 2)  NULL,
    reconciliation_mismatch           BOOLEAN         NULL,
    created_at                        TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_daily_summary_date UNIQUE (summary_date)
);

CREATE UNIQUE INDEX idx_daily_summary_date ON daily_summary (summary_date);