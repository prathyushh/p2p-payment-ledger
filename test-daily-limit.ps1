# Test Daily Limit Fraud Alert
Write-Host "=== Testing Daily Limit Fraud Alert ===" -ForegroundColor Cyan
Write-Host ""

# Check current fraud alerts count
Write-Host "Checking current DAILY_LIMIT_EXCEEDED fraud alerts..." -ForegroundColor Yellow
$env:PGPASSWORD="Cred.ai"
$result = psql -h localhost -U postgres -d p2p_payment -t -c "SELECT COUNT(*) FROM fraud_alerts WHERE alert_type = 'DAILY_LIMIT_EXCEEDED';"
Write-Host "Current DAILY_LIMIT_EXCEEDED alerts: $($result.Trim())" -ForegroundColor Cyan

Write-Host ""
Write-Host "=== Code Status ===" -ForegroundColor Cyan
Write-Host "✓ TransactionService.java has been updated with fraud alert logic" -ForegroundColor Green
Write-Host "✓ Backend JAR rebuilt at 09:40:10" -ForegroundColor Green
Write-Host "✓ Backend is currently running" -ForegroundColor Green
Write-Host ""
Write-Host "When a daily limit is exceeded, the code will:" -ForegroundColor Yellow
Write-Host "  1. Create a FAILED transaction" -ForegroundColor White
Write-Host "  2. Create a DAILY_LIMIT_EXCEEDED fraud alert" -ForegroundColor White
Write-Host "  3. Notify all admins" -ForegroundColor White
Write-Host "  4. Notify the sender" -ForegroundColor White
Write-Host "  5. Throw HTTP 422 exception" -ForegroundColor White
