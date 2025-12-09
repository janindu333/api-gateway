@echo off
title Kill Port 8761
color 0A
echo.
echo ============================================================
echo           KILLING PROCESS ON PORT 8761
echo ============================================================
echo.

echo Step 1: Checking for processes on port 8761...
echo.
netstat -ano | findstr :8761
echo.

echo Step 2: Finding and killing process...
echo.
set FOUND=0

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8761 ^| findstr LISTENING') do (
    echo [FOUND] Process ID: %%a
    echo [ACTION] Attempting to kill process...
    taskkill /PID %%a /F >nul 2>&1
    if %errorlevel% equ 0 (
        echo [SUCCESS] Process %%a has been killed!
        set FOUND=1
    ) else (
        echo [ERROR] Failed to kill process %%a
        echo [INFO] Try running this script as Administrator
    )
    echo.
)

if %FOUND% equ 0 (
    echo [INFO] No process found in LISTENING state on port 8761
    echo [INFO] Checking all states...
    echo.
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8761') do (
        echo [FOUND] Process ID: %%a
        echo [ACTION] Attempting to kill process...
        taskkill /PID %%a /F >nul 2>&1
        if %errorlevel% equ 0 (
            echo [SUCCESS] Process %%a has been killed!
            set FOUND=1
        )
        echo.
    )
)

echo ============================================================
echo Step 3: Verifying port is free...
echo ============================================================
echo.
netstat -ano | findstr :8761
if %errorlevel% equ 0 (
    echo.
    echo [WARNING] Port 8761 is STILL in use!
    echo [ACTION] Please try:
    echo   1. Run this script as Administrator (Right-click ^> Run as Administrator)
    echo   2. Check Task Manager for Java processes
    echo   3. Restart your computer
) else (
    echo.
    echo ============================================================
    echo [SUCCESS] Port 8761 is now FREE!
    echo ============================================================
    echo.
    echo You can now start your DiscoveryServerApplication.
)

echo.
echo ============================================================
pause
