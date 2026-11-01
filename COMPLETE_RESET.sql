-- ============================================================
-- COMPLETE RESET SCRIPT FOR ACCOUNT LOCK ISSUE
-- ============================================================

-- 1. Show current state
SELECT 'BEFORE RESET - User State:' as info;
SELECT id, email, account_locked, locked_until, enabled FROM users WHERE email = 'prathyushwork888@gmail.com';

SELECT 'BEFORE RESET - Login Attempts:' as info;
SELECT COUNT(*) as total_attempts, 
       SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as successful,
       SUM(CASE WHEN success = false THEN 1 ELSE 0 END) as failed
FROM login_attempts 
WHERE user_id IN (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com');

-- 2. DELETE all login attempts for this user
DELETE FROM login_attempts 
WHERE user_id IN (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com');

-- 3. UNLOCK the user account completely
UPDATE users 
SET account_locked = false, 
    locked_until = NULL 
WHERE email = 'prathyushwork888@gmail.com';

-- 4. Verify the reset
SELECT 'AFTER RESET - User State:' as info;
SELECT id, email, account_locked, locked_until, enabled FROM users WHERE email = 'prathyushwork888@gmail.com';

SELECT 'AFTER RESET - Login Attempts:' as info;
SELECT COUNT(*) as total_attempts FROM login_attempts 
WHERE user_id IN (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com');
