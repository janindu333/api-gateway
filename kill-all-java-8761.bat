@echo off
echo ============================================================
echo KILLING ALL JAVA PROCESSES (Port 8761 Solution)
echo ============================================================
echo.
echo WARNING: This will kill ALL Java processes on your system!
echo This includes any IDE, browser Java, or other Java applications.
echo.
pause

echo.
echo Killing all Java processes...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul

echo.
echo Checking port 8761...
netstat -ano | findstr :8761
if %errorlevel% equ 0 (
    echo Port 8761 is still in use. Finding process...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8761') do (
        echo Killing PID: %%a
        taskkill /PID %%a /F
    )
) else (
    echo SUCCESS: Port 8761 is now free!
)

echo.
echo Done!
pause

