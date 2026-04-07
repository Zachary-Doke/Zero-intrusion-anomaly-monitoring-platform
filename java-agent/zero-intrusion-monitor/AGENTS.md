# Repository Guidelines

## 项目结构与模块组织
本仓库是 Maven 多模块项目：
- `agent/`：Java Agent 核心模块，包含 `premain` 入口、Byte Buddy 插桩、异常采集与异步上报。
- `demo-app/`：演示程序，用于触发异常并验证 Agent 行为。
- 根目录 `pom.xml`：聚合构建与统一依赖版本管理。

源码遵循 Maven 默认目录：
- `*/src/main/java/...`：生产代码。
- `*/src/test/java/...`：测试代码（当前较少，新增测试请放这里）。
- `*/target/`：构建产物目录，不要手工修改。

## 构建、测试与本地运行命令
- `mvn clean package -DskipTests`：构建全部模块并生成可运行 Jar。
- `mvn clean test`：执行所有模块测试。
- `mvn -pl agent -am package`：仅重建 Agent 模块及其依赖模块。
- `mvn -pl demo-app -am package`：仅重建 Demo 模块及其依赖模块。
- 挂载 Agent 运行 Demo：
  `java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;packages=com.github.monitor.demo -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar`

## 代码风格与命名规范
- 语言版本：Java 8（`maven.compiler.source/target=8`）。
- 文件编码：UTF-8（无 BOM）。
- 缩进：4 个空格，禁止 Tab。
- 包名：全小写（如 `com.github.monitor.agent`）。
- 类/接口：`PascalCase`；方法/字段：`camelCase`；常量：`UPPER_SNAKE_CASE`。
- 保持方法职责单一、逻辑简洁，优先组合而非堆叠超大工具类。

## 测试规范
- 推荐使用 JUnit，在各模块 `src/test/java` 下新增测试。
- 命名约定：单元测试使用 `*Test`，集成测试使用 `*IT`。
- Agent 相关改动至少覆盖：
  - 正常路径：无异常时不应上报；
  - 异常路径：异常应被捕获并入队；
  - 安全路径：序列化深度/循环引用保护生效。

## 提交与合并请求规范
当前目录未包含 `.git` 历史，建议采用 Conventional Commits：
- `feat(agent): add trace key whitelist`
- `fix(serializer): prevent circular ref overflow`

PR 需包含：
- 变更范围与动机；
- 影响模块；
- 验证步骤与执行命令；
- 行为变化的日志或示例输出；
- 关联任务/Issue（如有）。

## 安全与配置建议
- 禁止提交真实接口地址、密钥、令牌等敏感信息。
- 默认开启敏感字段脱敏（如 `sensitiveFields=password,token`）。
- 生产发布前校准采样率、队列容量和批量上报参数。
