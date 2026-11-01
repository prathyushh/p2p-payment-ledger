-- Run this SQL to completely reset lock status for your account

-- 1. Clear all login attempts for your user
DELETE FROM login_attempts WHERE user_id IN (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com');

-- 2. Unlock your account
UPDATE users 
SET account_locked = false, 
    locked_until = NULL 
WHERE email = 'prathyushwork888@gmail.com';

-- 3. Verify the changes
SELECT id, email, account_locked, locked_until FROM users WHERE email = 'prathyushwork888@gmail.com';
