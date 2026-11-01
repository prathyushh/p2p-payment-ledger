-- Check current user state
SELECT id, email, account_locked, locked_until, failed_login_attempts 
FROM users 
WHERE email = 'prathyushwork888@gmail.com';
