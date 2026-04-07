@echo off
echo Building project...
cd zero-intrusion-monitor
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo Build success!
echo Agent jar: zero-intrusion-monitor\agent\target\agent-1.0-SNAPSHOT.jar
echo Demo jar: zero-intrusion-monitor\demo-app\target\demo-app-1.0-SNAPSHOT.jar
cd ..
