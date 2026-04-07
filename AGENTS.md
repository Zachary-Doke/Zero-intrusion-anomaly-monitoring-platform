# Repository Guidelines

## 项目结构与模块组织
当前仓库不再只有两个 Java 子项目，主要内容如下：

- `java-agent/zero-intrusion-monitor/`：Java Agent 多模块工程，包含 `agent` 与 `demo-app`。
- `analyze-platform/platform-server/`：Spring Boot 后端服务，负责异常分析、查询与平台接口。
- `front/`：独立前端应用，当前技术栈为 React 18 + React Router + Vite。
- `stitch_clean_minimal_prd (1)/`：设计稿与原型资料目录，仅在明确需要处理设计资产时再修改。

Java 模块遵循 Maven 标准目录：
- `src/main/java`：生产代码
- `src/main/resources`：运行配置，例如 `application.yml`
- `src/test/java`：测试代码
- `target/`：构建产物，不要手工修改

前端模块遵循 Vite 常规目录：
- `src/`：页面、组件、状态与 API 封装
- `public/`：静态资源（如存在）
- `dist/`：前端构建产物，不要手工修改
- `node_modules/`：依赖目录，不要手工修改

其他注意事项：
- `java-agent/out/`、各模块 `target/`、`front/dist/` 都属于生成内容。
- 若任务未明确要求，不要改动设计快照、日志文件和构建输出。

## 构建、测试与开发命令
请在对应子目录执行命令。

- Agent 项目构建：`cd java-agent/zero-intrusion-monitor && mvn clean package -DskipTests`
- Agent 项目测试：`cd java-agent/zero-intrusion-monitor && mvn clean test`
- 仅构建 Agent 模块：`cd java-agent/zero-intrusion-monitor && mvn -pl agent -am package`
- 挂载 Agent 运行 Demo：
  `cd java-agent/zero-intrusion-monitor && java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;packages=com.github.monitor.demo -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar`
- 平台后端启动：`cd analyze-platform/platform-server && mvn spring-boot:run`
- 平台后端测试/打包：`cd analyze-platform/platform-server && mvn clean test` / `mvn clean package`
- 前端安装依赖：`cd front && npm install`
- 前端本地开发：`cd front && npm run dev`
- 前端生产构建：`cd front && npm run build`

Windows 辅助脚本位于 `java-agent/` 下，例如 `build.bat`、`run-demo.bat`、`verify-demo.bat`。

## 代码风格与命名规范
- 统一编码：UTF-8（无 BOM）。
- 缩进：4 个空格，禁止 Tab。
- Java 命名：包名 `lowercase`，类 `PascalCase`，方法/字段 `camelCase`，常量 `UPPER_SNAKE_CASE`。
- 前端命名：React 组件文件使用 `PascalCase`，工具模块与普通脚本使用 `camelCase`。
- 坚持 KISS、YAGNI、DRY、SOLID：方法保持短小，避免臃肿服务类、重复分支和过早抽象。
- 保持现有技术边界：Agent 模块使用 Java 8，平台后端使用 Java 17，前端使用现有 React + Vite 方案，不要无故切换框架。

## 测试与验证要求
- 优先补充自动化测试，测试代码放在对应模块的 `src/test/java` 下。
- Java 测试命名：单元测试 `*Test`，集成测试 `*IT`。
- Agent 相关改动至少覆盖：
  - 正常路径不应上报
  - 异常路径应被捕获并入队
  - 序列化深度、循环引用、大小限制等保护逻辑
- 平台后端相关改动至少覆盖：
  - controller-service-repository 主链路
  - 请求参数校验
  - 安全鉴权或接口兼容性影响点
- 前端改动至少执行 `npm run build` 做构建校验；若涉及页面行为变更，补充说明手工验证步骤。

## 提交与合并请求规范
当前工作区可能不是干净状态，提交前先确认仅包含目标改动。建议采用 Conventional Commits，例如：

- `feat(agent): add trace context filter`
- `fix(platform-server): handle empty fingerprint`
- `refactor(front): split dashboard layout shell`

PR 建议包含：
- 变更范围与动机
- 影响模块
- 验证命令
- 关键日志、接口响应或页面截图（如有行为变化）

## 安全与配置注意事项
- 禁止提交真实密钥、令牌、生产地址或敏感样本数据。
- 保持脱敏字段配置，例如 `password`、`token`、`sensitiveFields`。
- 发布前确认采样率、队列容量、批量上报和跨域配置符合目标环境。
- 未经明确要求，不要修改构建产物、依赖目录或自动生成文件。
