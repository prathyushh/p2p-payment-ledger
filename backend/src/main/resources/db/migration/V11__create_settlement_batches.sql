-- V11__create_settlement_batches.sql
-- Summary record per ACH settlement job run.
-- job_log_id links to the scheduled_job_log entry for that run.

CREATE TABLE settlement_batches (
    id                    BIGSERIAL       PRIMARY KEY,
    job_log_id            BIGINT          NULL REFERENCES scheduled_job_log (id) ON DELETE SET NULL,
    batch_date            DATE            NOT NULL,
    transactions_settled  INT             NOT NULL DEFAULT 0,
    transactions_failed   INT             NOT NULL DEFAULT 0,
    total_amount_settled  NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    created_at            TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_settlement_batches_batch_date  ON settlement_batches (batch_date);
CREATE INDEX idx_settlement_batches_job_log_id  ON settlement_batches (job_log_id);