# @box/demo-mock

Box **静态演示站**的完整前端 Mock 层：驱动真实的 `box-web` / `box-admin-web`，不依赖 `box-server`。

## 目标

- **UI 1:1**：与线上一致的双端应用。
- **功能可体验**：列表/详情/创建/编辑/发布/对话流式/工具事件等，在浏览器内完成闭环。
- **会话持久化**：状态写入 `sessionStorage`（刷新仍保留），控制台执行 `__BOX_DEMO_RESET__()` 恢复种子数据。

## 架构

| 模块 | 作用 |
|------|------|
| `consumer/store.ts` | C 端 API 路由与种子世界 |
| `admin/store.ts` | B 端 API 路由与种子世界 |
| `engine/genericRest.ts` | 未单独实现的 REST 路径 → 集合 CRUD 回退 |
| `engine/chatSimulator.ts` | 关键词回复 + 模拟 tool.start/end |
| `fetchDemo.ts` | 拦截 `fetch`（SSE 对话 + JSON API） |
| `adapter.ts` | 拦截 `axios` |

## 与真后端的差异

- 无真实 LLM / Embedding / ES；对话与检索为规则与模板模拟。
- 复杂链路（支付回调、OAuth 跳转、Webhook 外网）仅 UI + 成功态模拟。
- 个别冷门接口若仍报错，把 Network 路径反馈后可补专用 handler。

## 开发

演示模式：`box-web` / `box-admin-web` 使用 `npm run dev:demo`（`VITE_BOX_DEMO=true`）。
