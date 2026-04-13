# 技术栈与工程约束

## 后端

- 框架: Spring Boot 3.2.4
- 组件: Spring Web、Spring Data JPA、Validation、SpringDoc
- 数据库: PostgreSQL（运行时）
- 测试数据库: H2（`src/test/resources/application.yml`）
- JDK: 17

## Agent

- 技术: Java Agent + Byte Buddy
- JDK: 8（兼容 8~21 的运行场景）
- 上报方式: 异步 HTTP 批量上报

## 前端

- 框架: React 18 + React Router 6
- 构建工具: Vite 6
- Node: 使用现有 `package-lock.json` 对应版本

## 配置约束

- 后端数据库连接通过环境变量注入：
  - `PLATFORM_DB_URL`
  - `PLATFORM_DB_USERNAME`
  - `PLATFORM_DB_PASSWORD`
- 安全相关配置（token secret、账号、agent key）必须使用环境变量覆盖默认值。

## 工程规则

- 编码统一 UTF-8（无 BOM）。
- 生成产物目录（如 `target/`, `dist/`, `node_modules/`）不手工修改。
- 变更优先遵循 KISS、YAGNI、DRY、SOLID。
