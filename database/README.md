# 数据库初始化脚本

本目录提供 PostgreSQL 初始化脚本，覆盖建表、索引和默认配置数据。

## 文件说明

- `create_database.sql`: 可选建库脚本（需高权限连接执行）
- `schema.sql`: 建表与索引（幂等，可重复执行）
- `seed.sql`: 默认数据初始化（目前包含 `rule_settings` 默认行）
- `init.sql`: 统一入口，顺序执行 `schema.sql` + `seed.sql`

## 执行方式

在仓库根目录执行：

```bash
psql -h 127.0.0.1 -U postgres -d postgres -f database/create_database.sql
PGPASSWORD='postgres' psql -h 127.0.0.1 -U postgres -d platformdb -f database/init.sql
```

也可以拆分执行：

```bash
PGPASSWORD='postgres' psql -h 127.0.0.1 -U postgres -d platformdb -f database/schema.sql
PGPASSWORD='postgres' psql -h 127.0.0.1 -U postgres -d platformdb -f database/seed.sql
```

## 说明

- 脚本按 PostgreSQL 方言编写。
- 设计为可重复执行，不会重复插入默认 `rule_settings`。
- 生产环境建议在后续迭代引入版本化迁移（Flyway/Liquibase）。
