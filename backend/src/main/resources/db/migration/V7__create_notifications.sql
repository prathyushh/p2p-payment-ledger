-- V7__create_notifications.sql
-- In-app notifications for all major events.

CREATE TABLE notifications (
    id                     BIGSERIAL     PRIMARY KEY,
    user_id                BIGINT        NOT NULL REFERENCES users (id)        ON DELETE CASCADE,
    type                   VARCHAR(50)   NOT NULL,
    title                  VARCHAR(255)  NOT NULL,
    message                TEXT          NOT NULL,
    is_read                BOOLEAN       NOT NULL DEFAULT FALSE,
    related_transaction_id BIGINT        NULL     REFERENCES transactions (id) ON DELETE SET NULL,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id    ON notifications (user_id);
CREATE INDEX idx_notifications_is_read    ON notifications (user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);