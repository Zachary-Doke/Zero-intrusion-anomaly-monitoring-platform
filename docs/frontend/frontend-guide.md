# 前端开发指南

## 模块结构

- `src/App.jsx`: 路由与鉴权守卫
- `src/auth/`: 登录态上下文
- `src/pages/`: 页面组件
- `src/components/`: 通用组件
- `src/lib/api.js`: 后端接口封装

## 接口调用约定

- 所有接口走相对路径 `/api/**`。
- 开发态由 Vite 代理到 `http://127.0.0.1:8080`。
- 登录成功后将 token 存入 localStorage。
- 接口返回 `401` 时自动清理本地登录态并跳转 `/login`。

## 路由

- `/login`
- `/`
- `/exceptions`
- `/exceptions/:id`
- `/settings`（管理员）

## 本地开发

```bash
cd front
npm install
npm run dev
```

## 构建与发布

```bash
cd front
npm run build
```

上线前至少完成一次真实后端联调，确保权限与错误处理行为一致。
