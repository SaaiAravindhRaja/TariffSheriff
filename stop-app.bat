@echo off
REM TariffSheriff Application Stop Script for Windows
REM This script stops both backend and frontend services

echo 🛑 Stopping TariffSheriff Application...
echo =======================================

REM Kill Spring Boot processes
echo [INFO] Stopping backend services...
taskkill /f /im java.exe /fi "WINDOWTITLE eq TariffSheriff Backend*" 2>nul
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo csv ^| find "spring-boot"') do (
    taskkill /f /pid %%i 2>nul
)

REM Kill Node.js/Vite processes
echo [INFO] Stopping frontend services...
taskkill /f /im node.exe /fi "WINDOWTITLE eq TariffSheriff Frontend*" 2>nul
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq node.exe" /fo csv ^| find "vite"') do (
    taskkill /f /pid %%i 2>nul
)

REM Archive log files
if exist backend.log (
    echo [INFO] Archiving backend.log...
    for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set mydate=%%c%%a%%b
    for /f "tokens=1-2 delims=: " %%a in ('time /t') do set mytime=%%a%%b
    move backend.log backend_%mydate%_%mytime%.log >nul 2>&1
)

if exist frontend.log (
    echo [INFO] Archiving frontend.log...
    for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set mydate=%%c%%a%%b
    for /f "tokens=1-2 delims=: " %%a in ('time /t') do set mytime=%%a%%b
    move frontend.log frontend_%mydate%_%mytime%.log >nul 2>&1
)

echo [SUCCESS] Application stopped successfully!
echo.
echo 📝 Archived log files are available for review.
echo 🚀 To start the application again, run: start-app.bat
echo.
pause