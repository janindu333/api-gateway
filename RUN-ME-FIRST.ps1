# ===================================================================
# KILL PORT 8761 - RUN THIS FIRST
# ===================================================================
# Right-click this file and select "Run with PowerShell"
# OR copy-paste the commands below into PowerShell (Run as Admin)
# ===================================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "KILLING PROCESS ON PORT 8761" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Find process on port 8761
Write-Host "Searching for process on port 8761..." -ForegroundColor Yellow

$pids = @()

# Method 1: Get-NetTCPConnection
try {
    $connections = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
    if ($connections) {
        Write-Host "Found process(es) using Get-NetTCPConnection:" -ForegroundColor Green
        foreach ($conn in $connections) {
            $pid = $conn.OwningProcess
            Write-Host "  - PID: $pid (State: $($conn.State))" -ForegroundColor White
            $pids += $pid
        }
    }
} catch {
    Write-Host "Error with Get-NetTCPConnection: $_" -ForegroundColor Red
}

# Method 2: netstat
try {
    $netstatOutput = netstat -ano | Select-String ":8761"
    if ($netstatOutput) {
        Write-Host "Found process(es) using netstat:" -ForegroundColor Green
        foreach ($line in $netstatOutput) {
            $parts = $line -split '\s+'
            $pid = $parts[-1]
            if ($pid -match '^\d+$') {
                Write-Host "  - PID: $pid" -ForegroundColor White
                $pids += [int]$pid
            }
        }
    }
} catch {
    Write-Host "Error with netstat: $_" -ForegroundColor Red
}

# Remove duplicates
$pids = $pids | Select-Object -Unique

if ($pids.Count -eq 0) {
    Write-Host ""
    Write-Host "No process found on port 8761!" -ForegroundColor Yellow
    Write-Host "The port appears to be free, but you're still getting the error." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Possible solutions:" -ForegroundColor Yellow
    Write-Host "  1. Restart your IDE/terminal" -ForegroundColor White
    Write-Host "  2. Check if another DiscoveryServerApplication instance is running in your IDE" -ForegroundColor White
    Write-Host "  3. Try changing the port in application.yml temporarily" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "Killing process(es)..." -ForegroundColor Yellow
    Write-Host ""
    
    foreach ($pid in $pids) {
        Write-Host "Attempting to kill PID: $pid" -ForegroundColor Cyan
        
        # Get process info
        try {
            $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "  Process Name: $($proc.ProcessName)" -ForegroundColor Gray
                Write-Host "  Start Time: $($proc.StartTime)" -ForegroundColor Gray
            }
        } catch {
            Write-Host "  (Could not get process info)" -ForegroundColor Gray
        }
        
        # Kill process
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            Write-Host "  SUCCESS: Process $pid killed!" -ForegroundColor Green
        } catch {
            Write-Host "  Trying taskkill as alternative..." -ForegroundColor Yellow
            $result = taskkill /PID $pid /F 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  SUCCESS: Process $pid killed via taskkill!" -ForegroundColor Green
            } else {
                Write-Host "  ERROR: Could not kill process $pid" -ForegroundColor Red
                Write-Host "  Try running PowerShell as Administrator" -ForegroundColor Yellow
            }
        }
        Write-Host ""
    }
    
    # Verify
    Write-Host "Verifying port is free..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    $finalCheck = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
    if ($finalCheck) {
        Write-Host ""
        Write-Host "WARNING: Port 8761 is still in use!" -ForegroundColor Red
        Write-Host "Remaining connections:" -ForegroundColor Red
        $finalCheck | Format-Table LocalPort, State, OwningProcess
    } else {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "SUCCESS! Port 8761 is now FREE!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "You can now start your DiscoveryServerApplication." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

