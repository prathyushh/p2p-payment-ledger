-- V6__create_fraud_alerts.sql
-- One alert per transaction (one-to-one or one-to-many for the HIGH_VALUE case).
-- reviewed_by FK references users.id (admin who acted).

CREATE TABLE fraud_alerts (
    id             BIGSERIAL    PRIMARY KEY,
    transaction_id BIGINT       NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    alert_type     VARCHAR(30)  NOT NULL,
    status         VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    reviewed_by    BIGINT       NULL REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at    TIMESTAMP    NULL,
    notes          TEXT         NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fraud_alert_type   CHECK (alert_type IN ('DAILY_LIMIT_EXCEEDED', 'DUPLICATE_TRANSACTION', 'HIGH_VALUE')),
    CONSTRAINT chk_fraud_alert_status CHECK (status      IN ('PENDING', 'BLOCKED', 'DISMISSED'))
);

CREATE INDEX idx_fraud_alerts_transaction_id ON fraud_alerts (transaction_id);
CREATE INDEX idx_fraud_alerts_status         ON fraud_alerts (status);
CREATE INDEX idx_fraud_alerts_alert_type     ON fraud_alerts (alert_type);
CREATE INDEX idx_fraud_alerts_created_at     ON fraud_alerts (created_at DESC);