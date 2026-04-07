# front

异常分析平台前端，当前为 `React 18 + Vite` 工程，用于承载登录、异常总览、异常列表、异常详情和配置管理。

## 技术栈

- React 18
- React Router 6
- Vite 6

## 当前功能

- 登录鉴权与本地登录态持久化
- 异常概览页
- 异常列表筛选与分页查询
- 异常详情查看、状态流转与处理建议生成
- 管理员配置页
- Agent 同步状态查看

## 目录说明

- `src/App.jsx`：路由入口与鉴权守卫
- `src/auth/`：登录态管理
- `src/pages/`：页面级组件
- `src/components/`：通用展示组件
- `src/lib/api.js`：前端接口封装

## 本地开发

```bash
cd front
npm install
npm run dev
```

默认开发地址为 `http://127.0.0.1:5173`。

当前 Vite 开发服务器会将 `/api` 请求代理到 `http://127.0.0.1:8080`。

## 生产构建

```bash
cd front
npm install
npm run build
```

如需本地预览构建结果，可执行：

```bash
cd front
npm run preview
```

## 页面路由

- `/login`：登录页
- `/`：异常概览页
- `/exceptions`：异常列表页
- `/exceptions/:id`：异常详情页
- `/settings`：配置页，仅管理员可访问

## 开发说明

- 前端接口默认走相对路径 `/api/**`，本地开发通过 Vite 代理转发到后端。
- 当接口返回 `401` 时，前端会清理本地登录态并跳转回登录页。
- 本项目当前没有配置自动化前端测试；改动后至少执行一次 `npm run build` 做构建校验。
