#!/usr/bin/env pwsh
# DIAGNOSTIC SCRIPT - Run this to check everything

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LOGIN LOCK DIAGNOSTIC SCRIPT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check if backend is running
Write-Host "1. Checking if backend is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -ErrorAction SilentlyContinue
    Write-Host "   ✓ Backend is running" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Backend is NOT running or health endpoint not available" -ForegroundColor Red
    Write-Host "   Start backend first: cd backend && mvn spring-boot:run" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. Checking database connection..." -ForegroundColor Yellow
Write-Host "   Run this SQL in pgAdmin/psql:" -ForegroundColor White
Write-Host @"
   
   -- Check user state
   SELECT id, email, account_locked, locked_until 
   FROM users 
   WHERE email = 'prathyushwork888@gmail.com';
   
   -- Count login attempts
   SELECT success, COUNT(*) 
   FROM login_attempts 
   WHERE user_id = (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com')
   GROUP BY success;
"@ -ForegroundColor Cyan

Write-Host ""
Write-Host "3. To reset your account, run this SQL:" -ForegroundColor Yellow
Write-Host @"
   
   DELETE FROM login_attempts 
   WHERE user_id = (SELECT id FROM users WHERE email = 'prathyushwork888@gmail.com');
   
   UPDATE users 
   SET account_locked = false, locked_until = NULL 
   WHERE email = 'prathyushwork888@gmail.com';
"@ -ForegroundColor Green

Write-Host ""
Write-Host "4. After resetting, restart backend and watch console for:" -ForegroundColor Yellow
Write-Host "   [LOGIN-ATTEMPT-CONFIG] Max Failed Attempts: 5" -ForegroundColor White
Write-Host "   [LOGIN-ATTEMPT-CONFIG] Lockout Minutes: 30" -ForegroundColor White
Write-Host ""
Write-Host "5. Test login with wrong password and watch for:" -ForegroundColor Yellow
Write-Host "   [LOGIN-ATTEMPT] Consecutive failures for user ...: 1/5" -ForegroundColor White
Write-Host "   [LOGIN-ATTEMPT] Consecutive failures for user ...: 2/5" -ForegroundColor White
Write-Host "   etc..." -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
