@echo off
REM K6 Load Test Runner for Windows
REM This script runs baseline and spike test scenarios sequentially

echo ========================================
echo K6 Load Test - Baseline + Spike
echo ========================================
echo.

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

REM Get timestamp for result files
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/: " %%a in ('time /t') do (set mytime=%%a%%b)
set timestamp=%mydate%_%mytime%

echo Starting load tests at %timestamp%
echo.

REM Scenario 1: Baseline Test
echo [1/2] Running Baseline Test...
docker run --rm -i --network host -v "%cd%:/k6" grafana/k6 run /k6/scenarios/baseline.js --out json=/k6/results/baseline_%timestamp%.json
echo Baseline test completed.
echo Waiting 60 seconds for system stabilization...
timeout /t 60 /nobreak > nul
echo.

REM Scenario 2: Spike Test
echo [2/2] Running Spike Test...
docker run --rm -i --network host -v "%cd%:/k6" grafana/k6 run /k6/scenarios/spike.js --out json=/k6/results/spike_%timestamp%.json
echo Spike test completed.
echo.

echo ========================================
echo All tests completed!
echo Results saved in: results\*_%timestamp%.json
echo ========================================
pause
