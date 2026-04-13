# 运维 Runbook

## 适用范围

用于平台后端与前端联动场景的日常巡检与故障排查。

## 日常巡检

- 检查后端进程与端口 `8080`。
- 检查前端服务可达性与 `/api` 代理行为。
- 检查 PostgreSQL 连接池、慢 SQL、磁盘容量。

## 常见故障处理

### 1. 登录失败（401）

排查步骤：

1. 确认 `PLATFORM_ADMIN_USERNAME/PLATFORM_ADMIN_PASSWORD` 是否被覆盖。
2. 检查 token secret 是否在集群节点一致。
3. 核对前端是否携带 `Authorization: Bearer <token>`。

### 2. Agent 上报失败（401/403）

排查步骤：

1. 校验 `X-Agent-Key`。
2. 检查后端 `platform.auth.agent-api-key` 实际生效值。
3. 检查 Agent endpoint 地址是否正确。

### 3. 后端启动失败（数据库连接）

排查步骤：

1. 校验 `PLATFORM_DB_URL/USERNAME/PASSWORD`。
2. 使用数据库客户端验证网络连通。
3. 查看后端启动日志中的 JDBC 异常。

### 4. AI 建议未生成

排查步骤：

1. 检查 `DEEPSEEK_API_KEY` 是否配置。
2. 检查规则配置中的 `aiBaseUrl` 与 `aiModel`。
3. 查看后端日志是否触发降级逻辑。

## 回滚建议

- 应用回滚：切换至上一稳定镜像版本。
- 配置回滚：恢复上一个可用配置集。
- 数据层回滚：以备份与迁移脚本策略为准。
