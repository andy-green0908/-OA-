# Nexus Office Frontend

Vue 3 + TypeScript + Vite前端，包含员工端、管理端和 AI办公助手界面。开发请求通过 Vite代理发送到本机网关 `18080`。

## 安装与运行

```powershell
npm install
npm run dev
```

本地地址：

```text
http://127.0.0.1:5173
```

## 生产构建

```powershell
npm run build
npm run preview -- --host 127.0.0.1 --port 5173
```

`npm run build` 会同时执行 Vue TypeScript检查和 Vite生产构建。

## 鉴权

- 管理端令牌保存在 `sessionStorage.adminToken`，请求头为 `X-Admin-Token`。
- 员工端令牌保存在 `sessionStorage.employeeToken`，请求头为 `X-Emp-Token`。
- AI请求复用员工令牌，由 AI服务向员工服务完成内部校验。

## AI 助手

- 健康状态显示聊天模型、RAG和向量就绪状态。
- 开场热门问题来自 AI服务加载的 MySQL知识库。
- 回答使用 SSE流式接口 `/api/v1/ai/chat/stream`。
- 退出登录后旧员工令牌立即失效。
