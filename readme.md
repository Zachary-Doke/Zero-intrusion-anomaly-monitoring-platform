# 仓库贡献指南

## 项目结构与模块组织
本仓库包含两个独立的 Java 子项目：
- `java-agent/zero-intrusion-monitor/`：Java Agent 多模块工程（`agent`、`demo-app`），用于异常捕获与异步上报。
- `analyze-platform/platform-server/`：基于 Spring Boot 的分析服务（REST、JPA、H2、OpenAPI）。

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
