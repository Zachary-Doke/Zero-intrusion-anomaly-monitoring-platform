@echo off
echo Running Demo App with Agent...

set AGENT_JAR=zero-intrusion-monitor\agent\target\agent-1.0-SNAPSHOT.jar
set DEMO_JAR=zero-intrusion-monitor\demo-app\target\demo-app-1.0-SNAPSHOT.jar

REM Example 1: Basic config
REM java -javaagent:%AGENT_JAR%=appName=MyDemo;serviceName=demo;packages=com.github.monitor.demo -jar %DEMO_JAR%

REM Example 2: Full config
java -javaagent:%AGENT_JAR%=appName=FullDemo;serviceName=demo;env=prod;packages=com.github.monitor.demo;endpoint=http://localhost:8080/report;sensitiveFields=password,token;sample.java.lang.IllegalArgumentException=1.0;collectionLimit=10 -jar %DEMO_JAR%
