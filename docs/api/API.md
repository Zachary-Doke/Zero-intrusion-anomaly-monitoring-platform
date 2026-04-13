# 异常分析平台 API 文档

- 版本: `1.0`
- 基线路径: `/api`
- 返回体: 统一使用 `Result<T>`，结构为 `{ code, message, data }`
- 文档刷新命令: `bash scripts/export_openapi.sh`

> 说明：本文件为初始基线文档。后端服务可用时，执行导出脚本会基于 `/v3/api-docs` 自动重建本文件。

## 鉴权约定

- 登录接口 `/api/auth/login`：免鉴权。
- Agent 上报相关接口：`X-Agent-Key: <agent-key>`。
- 其他接口：`Authorization: Bearer <token>`。

## Auth API

### POST `/api/auth/login`

- 说明：平台登录。
- 请求体：`LoginRequest`（`username`, `password`）
- 响应：`Result<LoginResponse>`

## Dashboard API

### GET `/api/dashboard/overview`

- 说明：获取总览仪表盘数据。
- 响应：`Result<ExceptionOverviewDto>`

## Exception Event API

### POST `/api/events`

- 说明：接收单条异常事件并存储。
- 鉴权：`X-Agent-Key`
- 请求体：`ExceptionEventReq`
- 响应：`Result<Void>`

### POST `/api/events/batch`

- 说明：批量接收异常事件并存储。
- 鉴权：`X-Agent-Key`
- 请求体：`List<ExceptionEventReq>`
- 响应：`Result<Void>`

## Exception Query API

### GET `/api/exceptions`

- 说明：分页查询异常事件。
- 查询参数：
  - `severity`（可选）
  - `status`（可选）
  - `serviceName`（可选）
  - `keyword`（可选）
  - `days`（可选）
  - `page` / `size` / `sort`（分页参数）
- 响应：`Result<Page<ExceptionListItemDto>>`

### GET `/api/exceptions/{id}`

- 说明：查询异常详情。
- 响应：`Result<ExceptionDetailDto>`

### PATCH `/api/exceptions/{id}/status`

- 说明：更新异常状态。
- 鉴权：`OPERATOR` 及以上
- 请求体：`StatusUpdateRequest`（`status`）
- 响应：`Result<ExceptionDetailDto>`

### POST `/api/exceptions/{id}/suggestion`

- 说明：生成处理建议。
- 鉴权：`OPERATOR` 及以上
- 响应：`Result<ExceptionSuggestionDto>`

### GET `/api/exceptions/fingerprints`

- 说明：查询指纹聚合结果。
- 响应：`Result<List<ExceptionFingerprint>>`

### GET `/api/exceptions/trends`

- 说明：查询异常趋势。
- 查询参数：`days`（可选）
- 响应：`Result<List<ExceptionTrendDto>>`

## Rule Settings API

### GET `/api/settings`

- 说明：查询规则配置。
- 鉴权：`ADMIN`
- 响应：`Result<RuleSettingsDto>`

### PUT `/api/settings`

- 说明：保存规则配置。
- 鉴权：`ADMIN`
- 请求体：`RuleSettingsDto`
- 响应：`Result<RuleSettingsDto>`

### GET `/api/settings/agent-runtime`

- 说明：Agent 拉取运行时配置。
- 鉴权：`X-Agent-Key`
- 查询参数：`serviceName`, `appName`
- 响应：`Result<AgentRuntimeConfigDto>`

### POST `/api/settings/agent-runtime/confirm`

- 说明：Agent 回传配置同步状态。
- 鉴权：`X-Agent-Key`
- 请求体：`AgentRuntimeConfirmRequest`
- 响应：`Result<Void>`

### GET `/api/settings/agent-sync-status`

- 说明：查询 Agent 配置同步状态。
- 鉴权：`ADMIN`
- 响应：`Result<List<AgentSyncStatusDto>>`

## 错误码约定

- `200`: 业务成功
- `401`: 未认证（token 无效或 agent key 错误）
- `403`: 已认证但权限不足
- `500`: 服务内部错误
