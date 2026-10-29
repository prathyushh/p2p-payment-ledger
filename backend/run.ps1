# run.ps1 — Local development launcher for the P2P Payment Ledger backend.
# Reads backend/.env, exports all key=value pairs as environment variables,
# then starts Spring Boot. Run this from the project root.
#
# Usage:
#   .\backend\run.ps1
#
# Requirements:
#   - backend/.env must exist (see .env.example for the template)
#   - Java 21 must be on PATH

$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path $envFile)) {
    Write-Error "backend/.env not found. Copy .env.example to backend/.env and fill in your values."
    exit 1
}

# Export each key=value pair as an environment variable (current process only)
Get-Content $envFile | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
}

$javaCmd = "C:\Users\Caytm\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe"
if (-not (Test-Path $javaCmd)) { $javaCmd = "java" }

Write-Host "Starting P2P Payment Ledger backend on http://localhost:8080 ..."
& $javaCmd -cp "$PSScriptRoot\.mvn\wrapper\maven-wrapper.jar" `
    "-Dmaven.multiModuleProjectDirectory=$PSScriptRoot" `
    org.apache.maven.wrapper.MavenWrapperMain `
    spring-boot:run