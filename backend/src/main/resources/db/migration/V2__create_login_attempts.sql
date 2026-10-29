-- V2__create_login_attempts.sql
-- Login attempt history for lockout detection.

CREATE TABLE login_attempts (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    attempt_time TIMESTAMP    NOT NULL DEFAULT NOW(),
    success      BOOLEAN      NOT NULL,
    ip_address   VARCHAR(45)  NULL
);

CREATE INDEX idx_login_attempts_user_time ON login_attempts (user_id, attempt_time DESC);