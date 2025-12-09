# Force kill process on port 8761
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "FORCE KILLING PROCESS ON PORT 8761" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Get all connections on port 8761
Write-Host "Step 1: Finding processes on port 8761..." -ForegroundColor Yellow
$pids = @()

# Method 1: Get-NetTCPConnection
$conns = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
if ($conns) {
    Write-Host "Found connections via Get-NetTCPConnection:" -ForegroundColor Green
    foreach ($conn in $conns) {
        $pid = $conn.OwningProcess
        Write-Host "  PID: $pid, State: $($conn.State)" -ForegroundColor White
        $pids += $pid
    }
}

# Method 2: netstat
Write-Host ""
Write-Host "Checking with netstat..." -ForegroundColor Yellow
$netstatOutput = netstat -ano | Select-String ":8761"
if ($netstatOutput) {
    Write-Host "Found connections via netstat:" -ForegroundColor Green
    foreach ($line in $netstatOutput) {
        $parts = $line -split '\s+'
        $pid = $parts[-1]
        if ($pid -match '^\d+$') {
            Write-Host "  PID: $pid" -ForegroundColor White
            $pids += [int]$pid
        }
    }
}

# Remove duplicates
$pids = $pids | Select-Object -Unique

if ($pids.Count -eq 0) {
    Write-Host ""
    Write-Host "No process found on port 8761. Port appears to be free." -ForegroundColor Green
    Write-Host ""
    Write-Host "However, if you're still getting the error, try:" -ForegroundColor Yellow
    Write-Host "  1. Restart your IDE/terminal" -ForegroundColor Yellow
    Write-Host "  2. Check if the process is in a different user session" -ForegroundColor Yellow
    Write-Host "  3. Restart your computer" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "Step 2: Killing processes..." -ForegroundColor Yellow
    foreach ($pid in $pids) {
        Write-Host ""
        Write-Host "Attempting to kill PID: $pid" -ForegroundColor Cyan
        
        # Get process info before killing
        $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  Process Name: $($proc.ProcessName)" -ForegroundColor White
            Write-Host "  Start Time: $($proc.StartTime)" -ForegroundColor White
        }
        
        # Try to kill
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            Write-Host "  SUCCESS: Process $pid killed!" -ForegroundColor Green
        } catch {
            Write-Host "  Trying taskkill as alternative..." -ForegroundColor Yellow
            $result = taskkill /PID $pid /F 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  SUCCESS: Process $pid killed via taskkill!" -ForegroundColor Green
            } else {
                Write-Host "  ERROR: Failed to kill process $pid" -ForegroundColor Red
                Write-Host "  You may need to run as Administrator" -ForegroundColor Yellow
            }
        }
    }
    
    Write-Host ""
    Write-Host "Step 3: Verifying port is free..." -ForegroundColor Yellow
    Start-Sleep -Seconds 1
    $finalCheck = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
    if ($finalCheck) {
        Write-Host ""
        Write-Host "WARNING: Port 8761 is still in use!" -ForegroundColor Red
        Write-Host "Remaining connections:" -ForegroundColor Red
        $finalCheck | Format-Table LocalPort, State, OwningProcess
        Write-Host ""
        Write-Host "Try running this script as Administrator" -ForegroundColor Yellow
    } else {
        Write-Host ""
        Write-Host "SUCCESS! Port 8761 is now FREE!" -ForegroundColor Green
        Write-Host "You can now start your DiscoveryServerApplication." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "Press any key to exit..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

