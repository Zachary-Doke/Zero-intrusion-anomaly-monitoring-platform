# Java Agent 接入指南

## 目标

在不改业务代码的前提下捕获异常，并将异常现场异步上报到分析平台。

## 构建

```bash
cd java-agent/zero-intrusion-monitor
mvn clean package -DskipTests
```

## 挂载示例

```bash
java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;serviceName=demo;packages=com.github.monitor.demo;endpoint=http://127.0.0.1:8080/api/events/batch -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar
```

## 关键参数

- `packages`: 监控包前缀（必填）
- `endpoint`: 后端接收地址
- `defaultSampleRate`: 默认采样率
- `depthLimit` / `lengthLimit` / `collectionLimit`: 序列化保护
- `sensitiveFields`: 脱敏字段
- `traceKeys`: MDC 链路字段

## 对接后端注意事项

- 后端事件接口需携带 `X-Agent-Key`。
- `endpoint` 建议使用 `/api/events/batch`。
- 生产环境应设置合理队列容量与批量大小，避免流量峰值丢数。

## 联调检查

1. 启动后端，确认 `/api/events` 可达。
2. 启动挂载 Agent 的业务应用。
3. 人工触发异常，确认前端可见异常记录。
4. 验证敏感字段已脱敏。
