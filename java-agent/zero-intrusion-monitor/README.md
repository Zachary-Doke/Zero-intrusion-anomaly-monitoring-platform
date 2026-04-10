# Zero-Intrusion Anomaly Monitoring Platform (Java Agent)

## 📖 项目简介 (Introduction)

本项目是一个基于 **Java Agent** 技术（`premain` 挂载）实现的零侵入异常现场捕获工具。它利用 **Byte Buddy** 进行字节码增强，能够在不修改业务代码的前提下，自动捕获方法抛出的异常，并收集丰富的现场上下文信息（如入参、堆栈、环境信息等），最终异步上报到指定服务端。

核心设计理念：**零侵入、高鲁棒性、低性能损耗**。

---

## ✨ 核心特性 (Features)

*   **零侵入挂载**：通过 `-javaagent` 启动参数挂载，对业务代码无感。
*   **按需监控**：支持配置包名前缀（`packages`），只监控特定业务包。
*   **智能捕获**：仅在方法抛出异常时触发上报，正常返回不采集。
*   **上下文采集**：自动捕获方法入参（Arguments）、异常堆栈、当前对象（This，可选）、环境信息（Host/Env/Pid）。
*   **安全序列化**：
    *   **深度限制**：防止深层对象图导致 StackOverflow（默认 5 层）。
    *   **长度/大小限制**：防止大字符串或大集合导致 OOM。
    *   **循环引用保护**：自动检测并截断循环引用。
    *   **异常吞没**：序列化过程中的任何异常都会被捕获并降级，绝不影响业务主流程。
*   **敏感数据脱敏**：支持按字段名（如 password, token）自动进行 SHA-256 哈希脱敏。
*   **异步高性能上报**：
    *   采集逻辑极轻（仅引用复制）。
    *   序列化与 HTTP 上报在独立的 Daemon 线程中异步执行。
    *   支持批量发送、队列缓冲、满队列丢弃策略。
*   **采样控制**：支持全局采样率及按异常类型配置采样率。

---

## 🏗️ 工程结构 (Project Structure)

```text
zero-intrusion-monitor/
├── agent/                    # [核心] Java Agent 模块
│   ├── src/main/java/com/github/monitor/agent/
│   │   ├── ZeroMonitorAgent.java   # Agent 入口 (premain)
│   │   ├── AgentConfig.java        # 配置解析
│   │   ├── ExceptionAdvice.java    # Byte Buddy 切面逻辑
│   │   ├── EventCollector.java     # 事件收集门面
│   │   ├── AsyncReporter.java      # 异步上报线程 & 队列
│   │   ├── SnapshotSerializer.java # 安全序列化与脱敏工具
│   │   └── Event.java              # 事件数据模型
│   └── pom.xml               # 包含 Maven Shade 插件配置
└── demo-app/                 # [示例] 测试应用模块
    └── src/main/java/com/github/monitor/demo/
        └── DemoApplication.java    # 模拟异常的演示程序
```

---

## 🚀 快速开始 (Quick Start)

### 1. 构建项目 (Build)

在项目根目录下执行：

```bash
mvn clean package -DskipTests
```

构建成功后，核心产物为：
*   Agent Jar: `agent/target/agent-1.0-SNAPSHOT.jar`
*   Demo Jar: `demo-app/target/demo-app-1.0-SNAPSHOT.jar`

### 2. 运行示例 (Run Demo)

使用以下命令运行 Demo 应用并挂载 Agent：

**基础运行（默认配置）：**
```bash
java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;serviceName=demo;packages=com.github.monitor.demo -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar
```

**高级运行（自定义配置）：**
```bash
java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=ProdApp;serviceName=demo;env=prod;packages=com.github.monitor.demo;endpoint=http://localhost:8080/api/events/batch;sensitiveFields=password,token;sample.java.lang.IllegalArgumentException=1.0;collectionLimit=10 -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar
```

---

## ⚙️ 配置参数说明 (Configuration)

Agent 参数通过 `-javaagent:path/to/agent.jar=key1=value1;key2=value2` 的形式传递。

| 参数名 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `appName` | unknown-app | 应用名称 |
| `serviceName` | `appName` | 服务名称；未传或为 `unknown-service` 时自动回落到 `appName` |
| `env` | dev | 环境名称 (dev/test/prod) |
| `packages` | (必填) | **监控包前缀**，多个用逗号分隔 (如 `com.myapp,org.test`) |
| `endpoint` | http://127.0.0.1:8080/api/events/batch | 异常上报 HTTP 接口地址 |
| `defaultSampleRate` | 1.0 | 默认采样率 (0.0 ~ 1.0) |
| `sample.<Exception>`| - | 按异常类型采样，如 `sample.java.lang.RuntimeException=0.5` |
| `queueCapacity` | 10000 | 异步队列容量 |
| `batchSize` | 10 | 批量上报的大小 |
| `flushIntervalMs` | 5000 | 批量上报的最大等待间隔 (毫秒) |
| `depthLimit` | 5 | 序列化最大深度 |
| `lengthLimit` | 4096 | 字符串最大长度 (超出截断) |
| `collectionLimit` | 50 | 集合/数组/Map 最大元素数量 (超出截断) |
| `sensitiveFields` | - | **脱敏字段名**，多个用逗号分隔 (如 `password,token`) |
| `traceKeys` | - | 需抓取的 MDC 上下文 Key (如 `traceId,requestId`) |
| `enableThisSnapshot`| false | 是否捕获 `this` 对象快照 (默认关闭以节省性能) |

---

## 📝 上报数据示例 (Data Schema)

Agent 会向 `endpoint` 发送如下 JSON 格式的异常事件：

```json
[
  {
    "timestamp": 1709630000123,
    "appName": "Demo",
    "env": "dev",
    "host": "my-computer",
    "pid": "12345",
    "threadName": "request-processor",
    "className": "com.github.monitor.demo.DemoApplication",
    "methodName": "processRequest",
    "methodDesc": "public static void com.github.monitor.demo.DemoApplication.processRequest()",
    "argumentsSnapshot": [],
    "thisSnapshot": null,
    "exceptionClass": "java.lang.IllegalArgumentException",
    "exceptionMessage": "Invalid argument with data: {id=123, sensitive=MASKED(SHA256:8d969eef...), name=test-user}",
    "stacktrace": "java.lang.IllegalArgumentException: ...\n\tat com.github.monitor.demo.DemoApplication.processRequest(DemoApplication.java:25)...",
    "fingerprint": "java.lang.IllegalArgumentException:com.github.monitor.demo.DemoApplication.processRequest",
    "traceContext": {
      "traceId": "abc-123"
    },
    "queueSize": 5,
    "droppedCount": 0
  }
]
```

---

## 🛠️ 本地验证 Checklist

1.  **插桩生效**：启动应用时，控制台输出 `[MonitorAgent] Starting...`。
2.  **异常捕获**：Demo 应用抛出异常时，Agent 不报错，且尝试上报。
3.  **脱敏生效**：配置 `sensitiveFields=password`，验证上报 JSON 中 password 字段是否为 `MASKED(...)`。
4.  **深度限制**：构造深层对象或循环引用，验证是否被截断或标记 `CIRCULAR_REF`。
5.  **异步队列**：大量异常并发时，业务线程不阻塞，多余异常被丢弃并记录 `droppedCount`。

---

## ⚠️ 注意事项

*   **JDK 版本**：支持 JDK 8 - 21。
*   **类加载器**：Agent 使用 `AgentBuilder`，默认通过 AppClassLoader 加载被监控类。对于自定义 ClassLoader 加载的类，可能需要调整 Agent 的匹配策略。
*   **性能影响**：虽然已做异步处理，但开启 `enableThisSnapshot` 或设置过大的 `depthLimit` 仍可能增加 CPU 和内存开销，生产环境请谨慎配置。
