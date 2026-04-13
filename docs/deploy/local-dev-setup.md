# 本地开发部署指南

## 前置条件

- JDK 17（platform-server）
- Maven 3.8+
- Node.js 18+
- PostgreSQL 14+

## 1. 配置数据库

创建数据库（可选使用脚本）：

```bash
psql -h 127.0.0.1 -U postgres -d postgres -f database/create_database.sql
```

执行初始化脚本（建表与默认配置）：

```bash
PGPASSWORD='postgres' psql -h 127.0.0.1 -U postgres -d platformdb -f database/init.sql
```

设置环境变量（Linux/macOS 示例）：

```bash
export PLATFORM_DB_URL='jdbc:postgresql://127.0.0.1:5432/platformdb'
export PLATFORM_DB_USERNAME='postgres'
export PLATFORM_DB_PASSWORD='postgres'
```

## 2. 启动后端

```bash
cd analyze-platform/platform-server
mvn spring-boot:run
```

启动成功后可访问：

- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI: `http://127.0.0.1:8080/v3/api-docs`

## 3. 启动前端

```bash
cd front
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5173`

## 4. 可选：启动 Agent Demo

```bash
cd java-agent/zero-intrusion-monitor
mvn clean package -DskipTests
java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;serviceName=demo;packages=com.github.monitor.demo -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar
```

## 常见问题

- 后端启动失败且提示数据库连接异常：检查 `PLATFORM_DB_*` 是否设置正确。
- 前端接口 401：检查是否已登录，或后端 token secret 是否变更。
- Agent 上报 401：检查 `X-Agent-Key` 与 `PLATFORM_AGENT_API_KEY` 是否一致。
