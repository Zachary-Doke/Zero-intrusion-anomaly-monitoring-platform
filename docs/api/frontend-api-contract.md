# 前后端接口契约（front <-> platform-server）

## 基础约定

- 前端调用统一使用相对路径 `/api/**`。
- 开发态通过 Vite 代理到 `http://127.0.0.1:8080`。
- 统一返回格式：`{ code, message, data }`。
- 当 HTTP 状态为 `401` 时，前端清理登录态并跳转 `/login`。

## 鉴权头约定

- 用户态接口：`Authorization: Bearer <token>`
- Agent 接口：`X-Agent-Key: <agent-key>`

## 前端已使用接口映射

### 登录与鉴权

- `POST /api/auth/login` -> `loginRequest(credentials)`

### 看板

- `GET /api/dashboard/overview` -> `api.getOverview()`

### 异常查询

- `GET /api/exceptions` -> `api.getExceptions(filters)`
- `GET /api/exceptions/{id}` -> `api.getExceptionDetail(id)`
- `PATCH /api/exceptions/{id}/status` -> `api.updateExceptionStatus(id, status)`
- `POST /api/exceptions/{id}/suggestion` -> `api.generateSuggestion(id)`

### 配置管理

- `GET /api/settings` -> `api.getSettings()`
- `PUT /api/settings` -> `api.saveSettings(settings)`
- `GET /api/settings/agent-sync-status` -> `api.getAgentSyncStatuses()`

## 字段兼容策略

- 后端新增字段应保持向后兼容，不移除前端已消费字段。
- 后端错误信息允许为空，前端使用兜底中文提示。
- 对 `data` 结构变更时需同步更新 `front/src/lib/api.js` 与页面消费逻辑。

## 联调检查

1. 登录后 token 能写入 localStorage。
2. 访问受保护页面时，token 失效会回到登录页。
3. 列表、详情、状态更新、建议生成链路全部可用。
4. 配置页仅管理员可访问。
