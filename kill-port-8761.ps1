Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Killing process on port 8761" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Method 1: Using Get-NetTCPConnection
Write-Host "Method 1: Checking with Get-NetTCPConnection..." -ForegroundColor Yellow
$connections = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
if ($connections) {
    foreach ($conn in $connections) {
        $pid = $conn.OwningProcess
        Write-Host "  Found process with PID: $pid" -ForegroundColor Green
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            Write-Host "  Successfully killed process $pid" -ForegroundColor Green
        } catch {
            Write-Host "  Failed to kill process $pid : $_" -ForegroundColor Red
        }
    }
} else {
    Write-Host "  No process found with Get-NetTCPConnection" -ForegroundColor Gray
}

Write-Host ""

# Method 2: Using netstat
Write-Host "Method 2: Checking with netstat..." -ForegroundColor Yellow
$netstatLines = netstat -ano | Select-String ":8761"
if ($netstatLines) {
    $pids = @()
    foreach ($line in $netstatLines) {
        $parts = $line -split '\s+'
        $pid = $parts[-1]
        if ($pid -match '^\d+$') {
            $pids += $pid
        }
    }
    $pids = $pids | Select-Object -Unique
    foreach ($pid in $pids) {
        Write-Host "  Found process with PID: $pid" -ForegroundColor Green
        try {
            taskkill /PID $pid /F 2>&1 | Out-Null
            Write-Host "  Successfully killed process $pid" -ForegroundColor Green
        } catch {
            Write-Host "  Failed to kill process $pid" -ForegroundColor Red
        }
    }
} else {
    Write-Host "  No process found with netstat" -ForegroundColor Gray
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Verifying port 8761 is free..." -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

$finalCheck = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
if ($finalCheck) {
    Write-Host "WARNING: Port 8761 is still in use!" -ForegroundColor Red
    $finalCheck | Format-Table LocalPort, State, OwningProcess
} else {
    Write-Host "SUCCESS: Port 8761 is now free!" -ForegroundColor Green
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

