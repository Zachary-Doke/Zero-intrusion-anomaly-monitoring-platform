# 文档中心

本目录用于维护零侵入异常监控平台的技术文档，覆盖开发、测试、部署、运维、前端与 Agent 接入。

## 快速入口

- 架构总览: [architecture/system-overview.md](architecture/system-overview.md)
- 技术栈与约束: [architecture/tech-stack-and-constraints.md](architecture/tech-stack-and-constraints.md)
- 后端 API: [api/API.md](api/API.md)
- 前端接口约定: [api/frontend-api-contract.md](api/frontend-api-contract.md)
- 数据库说明: [database/schema-and-data-flow.md](database/schema-and-data-flow.md)
- 本地部署: [deploy/local-dev-setup.md](deploy/local-dev-setup.md)
- 生产检查清单: [deploy/production-checklist.md](deploy/production-checklist.md)
- 运维 Runbook: [ops/runbook.md](ops/runbook.md)
- 测试策略: [testing/test-strategy.md](testing/test-strategy.md)
- 前端开发指南: [frontend/frontend-guide.md](frontend/frontend-guide.md)
- Agent 接入指南: [agent/agent-integration-guide.md](agent/agent-integration-guide.md)

## 角色阅读路径

### 后端开发

1. [architecture/system-overview.md](architecture/system-overview.md)
2. [database/schema-and-data-flow.md](database/schema-and-data-flow.md)
3. [api/API.md](api/API.md)
4. [testing/test-strategy.md](testing/test-strategy.md)

### 前端开发

1. [frontend/frontend-guide.md](frontend/frontend-guide.md)
2. [api/frontend-api-contract.md](api/frontend-api-contract.md)
3. [api/API.md](api/API.md)

### 运维/部署

1. [deploy/local-dev-setup.md](deploy/local-dev-setup.md)
2. [deploy/production-checklist.md](deploy/production-checklist.md)
3. [ops/runbook.md](ops/runbook.md)

## API 文档自动化

- OpenAPI 源地址: `http://127.0.0.1:8080/v3/api-docs`
- 导出命令: `bash scripts/export_openapi.sh`
- 输出产物:
  - `docs/api/openapi.json`
  - `docs/api/API.md`

如果本地后端未启动，脚本会给出明确提示。
