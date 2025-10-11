@echo off
REM TariffSheriff Application Startup Script for Windows
REM This script starts both backend and frontend services

setlocal enabledelayedexpansion

echo 🚀 Starting TariffSheriff Application...
echo ==================================

REM Function to check if a port is in use
:check_port
netstat -an | find ":%1 " | find "LISTENING" >nul
if %errorlevel% == 0 (
    exit /b 0
) else (
    exit /b 1
)

REM Check prerequisites
echo [INFO] Checking prerequisites...

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed. Please install Java 17 or higher.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven is not installed. Please install Maven.
    pause
    exit /b 1
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not installed. Please install Node.js.
    pause
    exit /b 1
)

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] npm is not installed. Please install npm.
    pause
    exit /b 1
)

echo [SUCCESS] All prerequisites are installed.

REM Install frontend dependencies if needed
if not exist "apps\frontend\node_modules" (
    echo [INFO] Installing frontend dependencies...
    cd apps\frontend
    call npm install
    cd ..\..
    echo [SUCCESS] Frontend dependencies installed.
) else (
    echo [INFO] Frontend dependencies already installed.
)

REM Start backend
echo [INFO] Starting backend service...
cd apps\backend

REM Start backend in background
echo [INFO] Compiling and starting Spring Boot application...
start "TariffSheriff Backend" /min cmd /c "mvn spring-boot:run > ..\backend.log 2>&1"

cd ..

REM Wait for backend to start
echo [INFO] Waiting for backend to start on port 8080...
set /a counter=0
:wait_backend
call :check_port 8080
if %errorlevel% == 0 (
    echo [SUCCESS] Backend started successfully!
    goto backend_ready
)
set /a counter+=1
if %counter% geq 60 (
    echo [ERROR] Backend failed to start within 60 seconds.
    echo [ERROR] Check backend.log for details.
    pause
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto wait_backend

:backend_ready

REM Start frontend
echo [INFO] Starting frontend service...
cd apps\frontend

REM Start frontend in background
echo [INFO] Starting Vite development server...
start "TariffSheriff Frontend" /min cmd /c "npm run dev > ..\frontend.log 2>&1"

cd ..

REM Wait for frontend to start
echo [INFO] Waiting for frontend to start on port 3000...
set /a counter=0
:wait_frontend
call :check_port 3000
if %errorlevel% == 0 (
    echo [SUCCESS] Frontend started successfully!
    goto frontend_ready
)
set /a counter+=1
if %counter% geq 30 (
    echo [ERROR] Frontend failed to start within 30 seconds.
    echo [ERROR] Check frontend.log for details.
    pause
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto wait_frontend

:frontend_ready

REM Display running services
echo.
echo 🎉 TariffSheriff Application is now running!
echo ==========================================
echo.
echo 📱 Frontend:     http://localhost:3000
echo 🔧 Backend API:  http://localhost:8080/api
echo 📚 Swagger UI:   http://localhost:8080/swagger-ui.html
echo 🤖 AI Assistant: http://localhost:3000/ai-assistant
echo.
echo 📝 Logs:
echo    Backend:  type backend.log
echo    Frontend: type frontend.log
echo.
echo 🛑 To stop the application:
echo    Run stop-app.bat or close the backend/frontend windows
echo.
echo Press any key to open the application in your browser...
pause >nul

REM Open application in default browser
start http://localhost:3000

echo Application opened in browser. Keep this window open to monitor the application.
echo Press any key to exit this script (services will continue running)...
pause >nul