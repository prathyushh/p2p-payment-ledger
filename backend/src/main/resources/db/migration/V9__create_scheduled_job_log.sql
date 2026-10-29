-- V9__create_scheduled_job_log.sql
-- Logs every scheduled job run with timing, record counts, and errors.

CREATE TABLE scheduled_job_log (
    id                BIGSERIAL       PRIMARY KEY,
    job_name          VARCHAR(100)    NOT NULL,
    started_at        TIMESTAMP       NOT NULL,
    ended_at          TIMESTAMP       NULL,
    status            VARCHAR(10)     NOT NULL DEFAULT 'RUNNING',
    records_processed INT             NULL,
    records_failed    INT             NULL,
    total_amount      NUMERIC(15, 2)  NULL,
    error_message     TEXT            NULL,
    CONSTRAINT chk_job_log_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_job_log_job_name   ON scheduled_job_log (job_name);
CREATE INDEX idx_job_log_started_at ON scheduled_job_log (started_at DESC);