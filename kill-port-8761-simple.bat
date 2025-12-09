@echo off
echo ====================================
echo Killing process on port 8761
echo ====================================
echo.

echo Checking for processes on port 8761...
netstat -ano | findstr :8761
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8761 ^| findstr LISTENING') do (
    echo.
    echo Found process with PID: %%a
    echo Killing process...
    taskkill /PID %%a /F
    if errorlevel 1 (
        echo Failed to kill process %%a
    ) else (
        echo Successfully killed process %%a
    )
)

echo.
echo ====================================
echo Final check:
echo ====================================
netstat -ano | findstr :8761
if errorlevel 1 (
    echo Port 8761 is now FREE!
) else (
    echo WARNING: Port 8761 is still in use!
    echo.
    echo Try running as Administrator or check manually:
    netstat -ano | findstr :8761
)

pause

