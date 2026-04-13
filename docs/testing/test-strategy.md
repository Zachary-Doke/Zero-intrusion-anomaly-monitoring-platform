# 测试策略与回归清单

## 后端测试

执行命令：

```bash
cd analyze-platform/platform-server
mvn clean test
```

当前策略：

- `@SpringBootTest` 集成测试覆盖主流程。
- 测试环境固定使用 H2，避免依赖外部 PostgreSQL。
- 重点覆盖：
  - 鉴权与角色访问控制
  - 异常上报与查询链路
  - 规则配置读写与版本递增
  - AI 建议生成与降级路径

## Agent 测试

执行命令：

```bash
cd java-agent/zero-intrusion-monitor
mvn clean test
```

重点覆盖：

- 正常路径不应上报
- 异常路径应被捕获并入队
- 序列化深度、循环引用、长度限制
- 脱敏字段处理

## 前端验证

执行命令：

```bash
cd front
npm run build
```

手工验证：

- 登录/登出
- 看板加载
- 异常列表筛选和分页
- 异常详情状态更新与建议生成
- 配置页权限控制

## 发布前最小回归

1. 后端测试全通过。
2. 前端构建成功。
3. API 冒烟：登录、总览、异常查询、状态变更、配置读取。
4. Agent 上报链路验证一次（单条或批量）。
