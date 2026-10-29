-- V8__create_audit_log.sql
-- Immutable audit trail for all admin actions. actor_id NULL means system-initiated.
-- details stored as JSONB for flexible before/after snapshots.

CREATE TABLE audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    actor_id    BIGINT       NULL REFERENCES users (id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    details     JSONB        NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_actor_id    ON audit_log (actor_id);
CREATE INDEX idx_audit_log_entity      ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at  ON audit_log (created_at DESC);