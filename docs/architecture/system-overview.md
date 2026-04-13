# 系统架构总览

## 目标

零侵入异常监控平台由 Java Agent、分析后端和 React 前端组成，目标是在不改业务代码的前提下完成异常采集、聚合、分析与处置。

## 模块划分

- `java-agent/zero-intrusion-monitor`: 运行时挂载，采集异常现场并上报。
- `analyze-platform/platform-server`: Spring Boot 后端，负责存储、查询、规则配置、AI 建议。
- `front`: React + Vite 前端，负责登录、看板、异常查询和规则管理。

## 核心调用链

1. 业务应用通过 `-javaagent` 挂载 Agent。
2. Agent 发现异常后调用 `/api/events` 或 `/api/events/batch` 上报。
3. 后端写入 `exception_event`，更新 `exception_fingerprint`，必要时触发 AI 分析并写入 `ai_analysis_result`。
4. 前端通过 `/api/**` 查询与操作数据，展示看板与处理详情。
5. Agent 通过 `/api/settings/agent-runtime` 拉取配置，通过 `/api/settings/agent-runtime/confirm` 回传同步状态。

## 鉴权模型

后端使用统一拦截器 `ApiAuthInterceptor` 对 `/api/**` 进行鉴权：

- 登录接口 `/api/auth/login` 免鉴权。
- Agent 上报与配置同步接口走 `X-Agent-Key`。
- 其他管理与查询接口走 `Authorization: Bearer <token>`。
- 角色分级：`VIEWER`、`OPERATOR`、`ADMIN`。

## 设计约束

- Agent 模块 Java 8，平台后端 Java 17。
- 后端运行时数据库 PostgreSQL，测试环境 H2。
- 前端开发态通过 Vite 代理 `/api` 到 `127.0.0.1:8080`。
