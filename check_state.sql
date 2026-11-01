-- Check current state of your user
SELECT id, email, account_locked, locked_until, enabled FROM users WHERE email = 'prathyushwork888@gmail.com';

-- Check all login attempts
SELECT id, user_id, attempt_time, success, ip_address FROM login_attempts WHERE user_id IN (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com') ORDER BY attempt_time DESC LIMIT 20;
