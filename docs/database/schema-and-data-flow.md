# 数据库结构与数据流

## 数据库选型

- 运行时: PostgreSQL（Hibernate Dialect: `PostgreSQLDialect`）
- 测试: H2 内存库（Dialect: `H2Dialect`）
- DDL 策略: `spring.jpa.hibernate.ddl-auto=update`

## 初始化脚本

- 目录: `database/`
- 入口脚本: `database/init.sql`
- 子脚本:
  - `database/create_database.sql`（可选建库）
  - `database/schema.sql`（建表与索引）
  - `database/seed.sql`（默认数据）

执行命令（仓库根目录）：

```bash
PGPASSWORD='postgres' psql -h 127.0.0.1 -U postgres -d platformdb -f database/init.sql
```

## 核心表

### `exception_event`

原始异常事件明细表，包含异常类型、栈、方法签名、trace 信息、Agent 同步信息等。

关键字段:

- `id` 主键（IDENTITY）
- `fingerprint` 指纹
- `severity` 严重级别
- `status` 处理状态
- `occurrence_time` 发生时间
- `stack_trace`、`arguments_snapshot`、`trace_context` 文本字段

### `exception_fingerprint`

异常指纹聚合表，用于列表统计、趋势与分组分析。

关键字段:

- `fingerprint` 主键
- `occurrence_count` 发生次数
- `first_seen` / `last_seen`
- `severity` / `status`

### `ai_analysis_result`

AI 分析结果表，用于保存根因与建议。

关键字段:

- `id` 主键（IDENTITY）
- `fingerprint` 唯一约束
- `report_status`
- `analysis_content`
- `requested_at` / `analysis_time`

### `rule_settings`

全局规则配置表（单行配置，默认 id=`default`）。

关键字段:

- 采样与序列化配置: `default_sample_rate`, `depth_limit`, `length_limit`, `collection_limit`
- 队列与上报配置: `queue_capacity`, `flush_interval_ms`
- AI 配置: `ai_base_url`, `ai_api_key`, `ai_model`, `ai_prompt_template`
- 脱敏与链路配置: `sensitive_fields`, `trace_keys`
- 版本字段: `version`

### `agent_sync_status`

Agent 配置同步状态表。

关键字段:

- `id` 主键
- `service_name`, `app_name`, `environment`
- `target_config_version`
- `last_confirmed_config_version`
- `last_config_sync_status`, `last_config_sync_error`

## 业务数据流

1. Agent 上报异常 -> 写 `exception_event`。
2. 后端按指纹聚合 -> 更新 `exception_fingerprint`。
3. 触发建议生成 -> 写/更新 `ai_analysis_result`。
4. 管理端更新规则 -> 更新 `rule_settings.version`。
5. Agent 拉取/回传同步状态 -> 更新 `agent_sync_status`。

## 维护建议

- 对高频查询字段建立索引（如 `exception_event.occurrence_time`、`severity`、`status`、`fingerprint`）。
- 保持 `rule_settings` 只有一个逻辑配置实例，避免并发覆盖。
- 生产环境建议引入迁移脚本工具（Flyway/Liquibase）替代 `ddl-auto=update`。
