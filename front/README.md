# front

异常分析平台前端，已升级为 `Vue 3 + Vite` 工程。

## 技术栈

- Vue 3
- Vite

## 功能

- 查看异常事件列表
- 查看异常指纹聚合
- 查看最近 7 天异常趋势
- 查看异常事件详情
- 触发并展示指定指纹的 AI 分析结果
- 在页面中切换后端地址

## 开发启动

```bash
cd front
npm install
npm run dev
```

默认开发地址一般是 `http://127.0.0.1:5173`。

## 生产构建

```bash
cd front
npm install
npm run build
```

## 说明

- 默认后端地址为 `http://127.0.0.1:8080`
- 后端已补充本地开发用 CORS 配置，可直接从 Vite 开发服务器访问 `/api/**`
