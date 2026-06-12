# ============================================================
# Notivio — One-Click Start Script
# Run this instead of: mvn spring-boot:run
# Usage: .\run.ps1
# ============================================================

Write-Host "Loading environment variables from .env..." -ForegroundColor Cyan

# Load all variables from .env into the current process
Get-Content .env | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '^\s*\w+\s*=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    Write-Host "  SET $key" -ForegroundColor DarkGray
}

# Force Java 21 (required — Java 26 breaks Lombok)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
$env:PATH      = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin;$env:PATH"

Write-Host ""
Write-Host "Java version: $(java -version 2>&1 | Select-Object -First 1)" -ForegroundColor Green
Write-Host "DB_URL      : $env:DB_URL"       -ForegroundColor Green
Write-Host "DB_USERNAME : $env:DB_USERNAME"  -ForegroundColor Green
Write-Host ""

# ── Pre-flight: check Supabase is reachable ──────────────────
$host_part = "aws-1-ap-south-1.pooler.supabase.com"
$port_part = 5432
Write-Host "Checking database connectivity ($host_part`:$port_part)..." -ForegroundColor Cyan
$tcp = Test-NetConnection -ComputerName $host_part -Port $port_part -WarningAction SilentlyContinue
if ($tcp.TcpTestSucceeded) {
    Write-Host "  [OK] Database host is reachable" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "  [ERROR] Cannot reach Supabase pooler at $host_part`:$port_part" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Possible causes:" -ForegroundColor Yellow
    Write-Host "    1. Your Supabase free-tier project is PAUSED" -ForegroundColor Yellow
    Write-Host "       -> Go to https://supabase.com/dashboard and resume it" -ForegroundColor Yellow
    Write-Host "    2. No internet connection" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Starting Notivio Backend..." -ForegroundColor Yellow
Write-Host "Swagger UI will be at: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host ""
Write-Host "NOTE: If you see 'ENOTFOUND tenant/user ... not found':" -ForegroundColor DarkYellow
Write-Host "  -> Your Supabase project is PAUSED. Resume it at:" -ForegroundColor DarkYellow
Write-Host "  -> https://supabase.com/dashboard" -ForegroundColor Cyan
Write-Host ""

mvn spring-boot:run
