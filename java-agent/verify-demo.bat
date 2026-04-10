@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0"
set "PROJECT_DIR=%BASE_DIR%zero-intrusion-monitor"
set "AGENT_JAR=%PROJECT_DIR%\agent\target\agent-1.0-SNAPSHOT.jar"
set "DEMO_JAR=%PROJECT_DIR%\demo-app\target\demo-app-1.0-SNAPSHOT.jar"
set "LOG_FILE=%PROJECT_DIR%\verify-demo.log"
set "LOG_OUT=%PROJECT_DIR%\verify-demo.stdout.log"
set "LOG_ERR=%PROJECT_DIR%\verify-demo.stderr.log"

echo [1/5] Checking prerequisites...
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] java not found in PATH.
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mvn not found in PATH.
  pause
  exit /b 1
)

echo [2/5] Building project...
pushd "%PROJECT_DIR%" || exit /b 1
call mvn clean package -DskipTests
if errorlevel 1 (
  echo [ERROR] Maven build failed.
  popd
  pause
  exit /b 1
)

if not exist "%AGENT_JAR%" (
  echo [ERROR] Agent jar not found: %AGENT_JAR%
  popd
  pause
  exit /b 1
)
if not exist "%DEMO_JAR%" (
  echo [ERROR] Demo jar not found: %DEMO_JAR%
  popd
  pause
  exit /b 1
)

echo [3/5] Running demo with agent for 15 seconds...
if exist "%LOG_FILE%" del /f /q "%LOG_FILE%"
if exist "%LOG_OUT%" del /f /q "%LOG_OUT%"
if exist "%LOG_ERR%" del /f /q "%LOG_ERR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$args = @('-javaagent:%AGENT_JAR%=appName=VerifyDemo;serviceName=demo;packages=com.github.monitor.demo;endpoint=http://127.0.0.1:65535/ingest;sensitiveFields=sensitive,password,token;collectionLimit=10','-jar','%DEMO_JAR%'); $p = Start-Process -FilePath 'java' -ArgumentList $args -RedirectStandardOutput '%LOG_OUT%' -RedirectStandardError '%LOG_ERR%' -PassThru; Start-Sleep -Seconds 15; if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }"

if exist "%LOG_OUT%" type "%LOG_OUT%" > "%LOG_FILE%"
if exist "%LOG_ERR%" type "%LOG_ERR%" >> "%LOG_FILE%"

if not exist "%LOG_FILE%" (
  echo [ERROR] Demo log not generated. Check Java startup and paths.
  echo stdout file: %LOG_OUT%
  echo stderr file: %LOG_ERR%
  popd
  pause
  exit /b 1
)

echo [4/5] Verifying logs...
set "AGENT_START_OK=0"
set "AGENT_READY_OK=0"
set "EXCEPTION_OK=0"

findstr /C:"[MonitorAgent] Starting..." "%LOG_FILE%" >nul && set "AGENT_START_OK=1"
findstr /C:"[MonitorAgent] Started successfully." "%LOG_FILE%" >nul && set "AGENT_READY_OK=1"
findstr /C:"Caught exception in main loop:" "%LOG_FILE%" >nul && set "EXCEPTION_OK=1"

echo Agent started log: !AGENT_START_OK!
echo Agent ready log:   !AGENT_READY_OK!
echo Exception caught:  !EXCEPTION_OK!

echo [5/5] Summary
if "!AGENT_START_OK!!AGENT_READY_OK!"=="11" (
  echo [PASS] Agent attached and instrumentation initialized.
) else (
  echo [FAIL] Agent startup logs missing.
)

if "!EXCEPTION_OK!"=="1" (
  echo [PASS] Demo exception loop observed.
) else (
  echo [WARN] No exception observed within 15s window. Re-run script.
)

echo Log file: %LOG_FILE%
echo --- Last 40 log lines ---
powershell -NoProfile -Command "if (Test-Path '%LOG_FILE%') { Get-Content '%LOG_FILE%' -Tail 40 }"

popd
pause
endlocal
