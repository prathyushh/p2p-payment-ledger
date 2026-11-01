-- V15__add_failed_login_counter.sql
-- Add a simple counter for failed login attempts to users table

ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_failed_login TIMESTAMP NULL;

CREATE INDEX idx_users_failed_attempts ON users (failed_login_attempts) WHERE failed_login_attempts > 0;

COMMENT ON COLUMN users.failed_login_attempts IS 'Counter for consecutive failed login attempts (reset on successful login)';
COMMENT ON COLUMN users.last_failed_login IS 'Timestamp of the most recent failed login attempt';
