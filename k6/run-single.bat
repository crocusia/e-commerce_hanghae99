@echo off
REM K6 Single Test Runner for Windows
REM Usage: run-single.bat [scenario-name]
REM Example: run-single.bat baseline

if "%~1"=="" (
    echo Usage: run-single.bat [scenario-name]
    echo.
    echo Available scenarios:
    echo   - baseline
    echo   - spike
    echo.
    pause
    exit /b 1
)

set SCENARIO=%~1

REM Check if Docker is running
docker info > nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Create results directory
if not exist "results" mkdir results

REM Get timestamp
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/: " %%a in ('time /t') do (set mytime=%%a%%b)
set timestamp=%mydate%_%mytime%

echo ========================================
echo K6 Load Test - %SCENARIO%
echo ========================================
echo.

docker run --rm -i --network host -v "%cd%:/k6" grafana/k6 run /k6/scenarios/%SCENARIO%.js --out json=/k6/results/%SCENARIO%_%timestamp%.json

echo.
echo Test completed!
echo Result saved: results\%SCENARIO%_%timestamp%.json
pause
