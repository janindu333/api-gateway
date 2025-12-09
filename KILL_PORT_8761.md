# How to Kill Process on Port 8761

## Quick Solutions

### Option 1: PowerShell One-Liner (Recommended)
Open PowerShell and run:
```powershell
Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### Option 2: CMD One-Liner
Open Command Prompt and run:
```cmd
for /f "tokens=5" %a in ('netstat -ano ^| findstr :8761') do taskkill /PID %a /F
```

### Option 3: Run the Batch File
Double-click or run in CMD:
```cmd
kill-port-8761-simple.bat
```

### Option 4: PowerShell Script
Run:
```powershell
powershell.exe -ExecutionPolicy Bypass -File kill-port-8761.ps1
```

### Option 5: Kill All Java Processes (Last Resort)
If nothing else works, this will kill ALL Java processes:
```powershell
Get-Process java* -ErrorAction SilentlyContinue | Stop-Process -Force
```

## Check if Port is Free
To verify the port is free, run:
```powershell
netstat -ano | findstr :8761
```
If nothing is returned, the port is free!

## If Still Not Working
1. Run your terminal/IDE as Administrator
2. Check if the process is running in a different user session
3. Restart your computer (last resort)

