-- Run this in PostgreSQL NOW to add the missing columns

-- Add the failed login counter columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_failed_login TIMESTAMP NULL;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_users_failed_attempts ON users (failed_login_attempts) WHERE failed_login_attempts > 0;

-- Reset all users to clean state
UPDATE users SET failed_login_attempts = 0, last_failed_login = NULL, account_locked = FALSE, locked_until = NULL;

-- Verify the columns exist
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'users' 
AND column_name IN ('failed_login_attempts', 'last_failed_login', 'account_locked', 'locked_until')
ORDER BY column_name;

-- Check your user
SELECT id, email, failed_login_attempts, account_locked, locked_until 
FROM users 
WHERE email = 'prathyushwork888@gmail.com';
