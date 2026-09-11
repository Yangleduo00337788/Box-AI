# Box V1 进度追踪

文档版本：V1.0 · 完成度：**100%**

---

## 总览

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 文档规划 | 100% | PRD / 架构 / 后端 / Runtime / 数据库 / 品牌 / 本文档 |
| 后端实现 | 100% | Phase 01–20 核心能力均已落地 |
| 前端实现 | 100% | C 端工作台 + Agent Builder + 资源管理页 |
| V1 可演示 | 100% | 注册→Agent→RAG/Tool→对话→发布→API 全链路 |

---

## 后端 Phase 01–20

| Phase | 内容 | 状态 |
|-------|------|------|
| 01 | 项目骨架 | ✅ |
| 02 | MySQL + Flyway (V1–V13) | ✅ |
| 03 | 用户注册 / 登录 | ✅ |
| 04 | Workspace | ✅ |
| 05 | RBAC / 成员角色 | ✅ |
| 06 | Agent CRUD | ✅ |
| 07 | Agent Version | ✅ |
| 08 | Model Provider | ✅ |
| 09 | LangChain4j | ✅ |
| 10 | Agent Runtime | ✅ |
| 11 | Conversation | ✅ |
| 12 | SSE Chat | ✅ |
| 13 | Knowledge | ✅ |
| 14 | RAG（ES 向量 + 关键词混合检索） | ✅ |
| 15 | Tool（HTTP CRUD + Agent Tool Calling） | ✅ |
| 16 | Workflow CRUD | ✅ |
| 17 | Workflow Runtime + Debug | ✅ |
| 18 | Trace / Execution | ✅ |
| 19 | Publish + API Key | ✅ |
| 20 | Analytics | ✅ |

### 本次补齐的关键能力

- **RAG**：文档分块 → Embedding → Elasticsearch 向量索引 → 混合检索注入 Agent Prompt
- **Tool Calling**：LangChain4j 多轮工具调用循环，绑定 HTTP Tool 即可在对话中触发
- **MCP**：`mcp_server` 表 + CRUD + 同步探测 API
- **Analytics**：执行次数、知识库/工具/工作流/MCP 统计

---

## 前端页面

| 页面 | 路由 | 状态 |
|------|------|------|
| 对话工作台 | `/chat` | ✅ |
| 智能体列表 | `/agents` | ✅ |
| Agent Builder | `/agents/:id/builder` | ✅ 含 Prompt/模型/知识库/工具/发布/调试 |
| 工作流列表 | `/workflows` | ✅ |
| 工作流编辑器 | `/workflows/:id/editor` | ✅ |
| 知识库 | `/knowledge` | ✅ |
| 工具 | `/tools` | ✅ |
| MCP | `/mcp` | ✅ |
| 模型 | `/models` | ✅ |
| 分析 | `/analytics` | ✅ |
| 插件市场 | `/plugin-market` | ✅ |
| 管理后台 | `box-admin-web` | ✅ |

---

## V1 Demo 验收路径

```
注册 / 登录
  → 创建 Workspace
  → 创建 Agent（Prompt + 模型）
  → 上传知识库文档（RAG）
  → 绑定 HTTP Tool（Tool Calling）
  → Builder 内 SSE 调试对话
  → 发布 Agent
  → Web Chat / Published API + API Key 调用
  → 查看 Analytics / Execution Trace
```

---

## 基础设施

| 组件 | 状态 |
|------|------|
| MySQL 8 | ✅ docker-compose |
| Redis | ✅ |
| Elasticsearch 8 | ✅ 知识库向量索引 |
| MinIO | ✅ 文档存储 |
| LangChain4j | ✅ Chat + Embedding + Tool |

---

## 文档索引

1. `01-Prompt.md` — 总纲
2. `02-PRD.md` — 产品需求
3. `03-Architecture.md` — 架构
4. `04-Database.md` / `07-MySQL.md` — 数据库
5. `05-Backend.md` — 后端与 Phase 顺序
6. `06-Runtime.md` — Runtime 设计
7. `08-Name&slogan.md` — 品牌
8. **`09-Progress.md`** — 本文档（进度 100%）
