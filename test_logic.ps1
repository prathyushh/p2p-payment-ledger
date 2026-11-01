# TEST THE CONSECUTIVE FAILURE LOGIC
# This simulates what happens in the code

Write-Host "SIMULATING CONSECUTIVE FAILURE COUNT LOGIC" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Simulate login attempts (most recent first)
$attempts = @(
    @{time="10:05"; success=$false},  # Current attempt (just saved)
    @{time="10:04"; success=$false},  # Previous attempt
    @{time="10:03"; success=$false},  
    @{time="10:02"; success=$false},  
    @{time="10:01"; success=$false},  
    @{time="09:50"; success=$true},   # Last successful login - STOP HERE
    @{time="09:45"; success=$false}   # Old failure - shouldn't count
)

Write-Host "Login Attempt History (newest first):"
$attempts | ForEach-Object { 
    Write-Host "  $($_.time) - Success: $($_.success)" 
}

Write-Host ""
Write-Host "Counting consecutive failures..." -ForegroundColor Yellow

$count = 0
foreach ($attempt in $attempts) {
    if ($attempt.success) {
        Write-Host "  → Found successful login at $($attempt.time), STOP counting" -ForegroundColor Green
        break
    }
    $count++
    Write-Host "  → Failure at $($attempt.time), count = $count" -ForegroundColor Red
}

Write-Host ""
Write-Host "RESULT: $count consecutive failures" -ForegroundColor Cyan
Write-Host "Max allowed: 5" -ForegroundColor Cyan

if ($count -ge 5) {
    Write-Host "ACTION: LOCK ACCOUNT" -ForegroundColor Red
} else {
    Write-Host "ACTION: Allow login attempt" -ForegroundColor Green
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
