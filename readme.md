# 仓库贡献指南

## 文档中心
- 总入口：[`docs/README.md`](docs/README.md)
- 后端 API：[`docs/api/API.md`](docs/api/API.md)
- 数据库说明：[`docs/database/schema-and-data-flow.md`](docs/database/schema-and-data-flow.md)
- 数据库脚本：[`database/README.md`](database/README.md)
- 本地部署指南：[`docs/deploy/local-dev-setup.md`](docs/deploy/local-dev-setup.md)
- 运维 Runbook：[`docs/ops/runbook.md`](docs/ops/runbook.md)
- 测试策略：[`docs/testing/test-strategy.md`](docs/testing/test-strategy.md)

## 项目结构与模块组织
本仓库包含多个子项目：
- `java-agent/zero-intrusion-monitor/`：Java Agent 多模块工程（`agent`、`demo-app`），用于异常捕获与异步上报。
- `analyze-platform/platform-server/`：基于 Spring Boot 的分析服务（REST、JPA、PostgreSQL、OpenAPI，测试使用 H2）。
- `front/`：基于 `React 18 + Vite` 的前端应用，负责登录、看板、异常查询与配置管理页面。

各模块遵循 Maven 标准目录：
- `src/main/java`：业务代码
- `src/main/resources`：配置资源
- `src/test/java`：测试代码
- `target/`：构建产物（禁止手工修改）

## 构建、测试与本地运行
在对应目录执行命令：
- Agent 全量构建：`cd java-agent/zero-intrusion-monitor && mvn clean package -DskipTests`
- Agent 测试：`cd java-agent/zero-intrusion-monitor && mvn clean test`
- 仅构建 agent 模块：`mvn -pl agent -am package`
- 挂载 Agent 运行 Demo：
  `java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;packages=com.github.monitor.demo -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar`
- 平台服务启动：`cd analyze-platform/platform-server && mvn spring-boot:run`
  - 需先设置环境变量：`PLATFORM_DB_URL`、`PLATFORM_DB_USERNAME`、`PLATFORM_DB_PASSWORD`
- 前端依赖安装：`cd front && npm install`
- 前端本地开发：`cd front && npm run dev`
- 前端生产构建：`cd front && npm run build`
- 前端构建预览：`cd front && npm run preview`
  - 前端开发默认地址：`http://127.0.0.1:5173`
  - 前端开发态会将 `/api` 代理到：`http://127.0.0.1:8080`

Windows 可使用 `java-agent/` 下脚本：`build.bat`、`run-demo.bat`、`verify-demo.bat`。

## 编码规范与命名
- 编码统一 UTF-8（无 BOM）。
- 使用 4 空格缩进，禁止 Tab。
- 包名小写，类名 `PascalCase`，方法/字段 `camelCase`，常量 `UPPER_SNAKE_CASE`。
- 遵循 KISS/YAGNI：优先小而清晰的方法，避免过度设计。

## 测试要求
- 使用 JUnit，测试放在 `src/test/java`。
- 命名约定：单元测试 `*Test`，集成测试 `*IT`。
- Agent 相关改动需覆盖：正常路径不上报、异常路径会上报、序列化深度/循环引用保护。

## 提交与 PR 规范
- 建议使用 Conventional Commits：
  - `feat(agent): 增加 trace 上下文过滤`
  - `fix(platform-server): 修复空指纹处理`
- PR 必须包含：变更范围、动机、影响模块、验证命令、关键日志或接口响应示例。

## 安全与配置
- 禁止提交密钥、令牌和真实生产地址。
- 建议开启敏感字段脱敏（如 `password,token`）。
- 生产前校准采样率、队列容量与批量上报参数。
