# Box V1 进度追踪

文档版本：V1.5 · 整体完成度：**~95%**

> 未完成项与执行顺序见 [`10-Gaps.md`](./10-Gaps.md)。本文档反映代码库真实状态，不再使用「100%」表述。

---

## 总览

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 文档规划 | ~96% | PRD / 架构 / 后端 / Runtime / 数据库 / 品牌；docker-compose 含 app profile |
| 后端实现 | ~96% | OAuth stub、Embed 配置 API、Docker 镜像 |
| 前端实现 | ~94% | Embed 定制、模板向导、Workflow 1920 布局 |
| V1 可演示 | ~98% | **P0/P1/P2 主清单已全部完成** |

---

## Sprint 6 已完成（P2 对齐）

| ID | 任务 | 状态 |
|----|------|------|
| B-17 | OAuth / SSO stub（providers + authorize/callback） | ✅ |
| B-18 | `box-server/Dockerfile` + compose profile `app` | ✅ |
| F-17 | Embed Web Chat 定制（Builder + 公开 embed-config） | ✅ |
| F-19 | CreateAgentDialog 模板向导 | ✅ |
| F-20 | Workflow 编辑器 1920px 布局优化 | ✅ |

**关键 API**：

- `GET /api/v1/auth/oauth/providers`
- `GET /api/v1/agents/{id}/embed-config` · `PUT ...`
- `GET /api/v1/published/agents/{id}/embed-config`（公开，无需 API Key）

**Docker 全栈**：`cd deploy && docker compose --profile app up -d --build`

---

## Sprint 5 已完成（P1 收尾）

| ID | 任务 | 状态 |
|----|------|------|
| B-15 | `box-analytics` 模块落地 | ✅ |
| D-11 | 文档维护规范 | ✅ |

**模块变更**：`AnalyticsController` / `AnalyticsApplicationService` 及 VO 从 `box-conversation` 迁至 `com.boxai.analytics`；`box-bootstrap` 新增 `box-analytics` 依赖；API 路径不变（`/api/v1/analytics/*`）。

---

## Sprint 4 已完成（体验收尾）

| ID | 任务 | 状态 |
|----|------|------|
| B-06 | Redis 限流（登录/Chat/API Key/Tool） | ✅ |
| B-13 | Function / Code Tool 运行时 | ✅ |
| B-14 | 危险 Tool 执行确认（`tool.confirm` + Redis 令牌） | ✅ |
| B-16 | 通知中心后端事件 | ✅ |
| F-02 | Agent 列表策略（方案 B：`/chat` 合并） | ✅ |
| F-06 | 默认入口 `/chat`，侧栏保留概览 | ✅ |
| F-07 | Settings IA（`/team` + `/settings/*`） | ✅ |
| F-11 | Chat Trace 侧栏 | ✅ |
| F-13 | 危险 Tool 二次确认 Dialog | ✅ |
| F-15 | 错误页 Request ID + 429 对齐 | ✅ |
| F-16 | 审计日志页 `/settings/audit-logs` | ✅ |
| F-18 | 通知中心 UI + 轮询 | ✅ |
| D-05 | docker-compose 说明修正 | ✅ |
| D-07 | 超前实现归档 | ✅ |
| D-10 | V1 Demo 验收路径 | ✅ |
| D-12 | 文档索引 | ✅ |

### 产品决策（F-02 / F-06 / F-07）

- **Agent 列表**：不恢复独立 `/agents` 列表页；`/agents` → `/chat`，在对话工作台选择/创建 Agent
- **默认入口**：登录后进入 `/chat`；`/dashboard` 作为可选「概览」
- **设置 IA**：成员与角色在 `/team`；API Key、审计、安全等在 `/settings/*`，按权限显隐

### 限流策略（B-06）

| 场景 | 维度 | 限额 |
|------|------|------|
| 登录 | IP | 20/min |
| Chat / Stream | workspace + user | 60/min |
| Published Chat | agent + IP | 60/min |
| API Key（`/published/*`） | api-key id | 120/min |
| Tool 测试 | workspace | 30/min |

---

## Sprint 3 已完成（Workflow + 分析）

| ID | 任务 | 状态 |
|----|------|------|
| B-07 | Workflow Switch 节点 | ✅ |
| B-08 | Workflow 图校验增强 | ✅ |
| B-09 | Code 节点沙箱加固 | ✅ |
| F-08 | Analytics ECharts 图表 | ✅ |
| F-14 | Switch 节点 UI | ✅ |
| D-04 | 模块结构文档修正 | ✅ |
| D-09 | API 清单抽检 | ✅ |

---

## Sprint 2 已完成（RAG + 可观测）

| ID | 任务 | 状态 |
|----|------|------|
| B-02 | Rerank 真正接入 | ✅ |
| B-10 | 文档处理状态机 | ✅ |
| B-11 | RAG Test Answer API | ✅ |
| B-12 | 流式 Tool Calling 事件 | ✅ |
| F-05 | 知识库处理进度 UI | ✅ |
| F-09 | Debug Console 三栏 | ✅ |
| F-10 | Chat 引用 Citation | ✅ |
| F-12 | RAG 测试页 | ✅ |
| D-06 | PRD P0/P1/P2 对照表 | ✅ |
| D-08 | Runtime 文档对齐 | ✅ |

---

## Sprint 1 已完成（闭环与安全）

| ID | 任务 | 状态 |
|----|------|------|
| B-01 | 审计日志 Audit Log | ✅ |
| B-03 | RBAC 统一校验 | ✅ |
| B-04 | 删除依赖检查（409 + 依赖列表） | ✅ |
| B-05 | Request ID 全链路 | ✅ |
| F-01 | 前端权限守卫 | ✅ |
| F-03 | 删除依赖提示 | ✅ |
| F-04 | API Key 仅展示一次 | ✅ |
| D-01~D-03 | 文档进度 / 路由 / Flyway 同步 | ✅ |

---

## 后端 Phase 01–20

| Phase | 内容 | 状态 |
|-------|------|------|
| 01 | 项目骨架 | ✅ |
| 02 | MySQL + Flyway（**V1–V24**，见下表） | ✅ |
| 03 | 用户注册 / 登录 | ✅ |
| 04 | Workspace | ✅ |
| 05 | RBAC / 成员角色 | ✅ |
| 06 | Agent CRUD | ✅ |
| 07 | Agent Version | ✅ |
| 08 | Model Provider | ✅ |
| 09 | LangChain4j | ✅ |
| 10 | Agent Runtime（`box-agent`） | ✅ |
| 11 | Conversation | ✅ |
| 12 | SSE Chat | ✅ |
| 13 | Knowledge | ✅ |
| 14 | RAG（ES 向量 + 关键词混合检索 + Rerank） | ✅ |
| 15 | Tool（HTTP CRUD + Agent Tool Calling + Function/Code） | ✅ |
| 16 | Workflow CRUD | ✅ |
| 17 | Workflow Runtime + Debug | ✅ Switch 节点 + 图校验已补齐 |
| 18 | Trace / Execution | ✅ Request ID 已贯通至 execution / 日志 |
| 19 | Publish + API Key | ✅ |
| 20 | Analytics | ✅ | `box-analytics` · `/analytics` |

### 关键能力备注

- **RAG**：文档分块 → Embedding → ES 索引 → 混合检索 + Rerank 模型重排
- **Tool Calling**：LangChain4j 多轮 HTTP Tool 调用；SSE 推送 `tool.start` / `tool.delta` / `tool.end`；MCP 已支持
- **安全**：审计日志、RBAC 权限码统一、资源删除依赖检查、Request ID 全链路、Redis 限流（429）
- **超前实现**：见下表「超前实现归档（D-07）」

---

## Flyway 迁移（当前 V1–V24）

路径：`box-server/box-bootstrap/src/main/resources/db/migration/`

| 版本 | 文件 | 主要内容 |
|------|------|----------|
| V1 | `V1__init_identity.sql` | 用户、租户身份 |
| V2 | `V2__model.sql` | 模型提供商与凭证 |
| V3 | `V3__agent.sql` | Agent 与版本 |
| V4 | `V4__conversation.sql` | 会话与消息 |
| V5 | `V5__tenant.sql` | 租户 |
| V6 | `V6__tenant_type.sql` | 租户类型 |
| V7 | `V7__plan_quota.sql` | 套餐与配额 |
| V8 | `V8__platform_model.sql` | 平台模型 |
| V9 | `V9__agent_market.sql` | Agent 市场 |
| V10 | `V10__consumer_portal.sql` | 消费者门户 |
| V11 | `V11__portal_enhancements.sql` | 门户增强 |
| V12 | `V12__platform_core.sql` | Workflow / Execution / Trace |
| V13 | `V13__mcp_server.sql` | MCP Server |
| V14 | `V14__agent_mcp_memory.sql` | Agent MCP 与记忆 |
| V15 | `V15__role_permissions.sql` | 角色权限 |
| V16 | `V16__quota_knowledge_plugin_resource.sql` | 配额与插件资源 |
| V17 | `V17__agent_long_term_memory.sql` | 长期记忆 |
| V18 | `V18__workflow_webhook.sql` | Workflow Webhook |
| V19 | `V19__tool_database_function_config.sql` | Tool DB/Function 配置 |
| V20 | `V20__agent_sub_agent.sql` | Sub Agent |
| V21 | `V21__notification.sql` | 通知 |
| V22 | `V22__audit_log.sql` | 审计日志 |
| V23 | `V23__rbac_permissions.sql` | RBAC 权限码补齐 |
| V24 | `V24__execution_request_id.sql` | Execution request_id |

---

## 前端页面与路由

默认入口：`/` → `/chat`（新任务 / 对话工作台）

| 页面 | 路由 | 状态 | 备注 |
|------|------|------|------|
| 对话工作台 | `/chat`、`/chat/:id` | ✅ | 登录后默认页 |
| 概览 | `/dashboard` | ✅ | |
| 智能体列表 | `/agents` | ↪️ | **重定向至 `/chat`**；列表与对话合并 |
| Agent Builder | `/agents/:id/builder` | ✅ | Prompt / 模型 / 知识库 / 工具 / 发布 / 调试 |
| 工作流列表 | `/workflows` | ✅ | 经插件市场或侧栏进入 |
| 工作流编辑器 | `/workflows/:id/editor` | ✅ | |
| 知识库 | `/knowledge` | ✅ | |
| 工具 | `/tools` | ✅ | |
| MCP | `/mcp` | ✅ | |
| 模型 | `/models` | ✅ | |
| 插件市场 | `/plugin-market` | ✅ | 工作流 / 知识库 / 工具 / MCP 入口聚合 |
| 执行记录 | `/executions` | ✅ | |
| Debug Console | `/debug` | ✅ | 三栏：Input / Trace / Output |
| 分析 | `/analytics` | ✅ | ECharts 趋势图 + Top Agents 柱状图 |
| 市场 | `/market` | ✅ | |
| 团队 | `/team` | ✅ | 需 `member:manage` |
| 设置 | `/settings/*` | ✅ | API 密钥 / 角色 / 审计日志等，按权限显隐 |
| 审计日志 | `/settings/audit-logs` | ✅ | 需 `audit:read` |
| 无权限 | `/forbidden` | ✅ | |
| 嵌入对话 | `/embed/agents/:id` | ✅ | 公开页 |
| 管理后台 | `box-admin-web` | ✅ | 独立应用 |

已废弃或重定向：`/conversations`、`/chat/logs` → `/chat`

---

## V1 Demo 验收路径

```
注册 / 登录
  → 默认进入 /chat（新任务）
  → 创建或选择 Agent → /agents/:id/builder
  → 配置 Prompt + 模型 + 知识库（RAG）+ HTTP Tool
  → Builder 内 SSE 调试对话
  → 发布 Agent
  → Web Chat / Published API + API Key（/settings/api-keys）
  → 查看 /executions、/debug 或 /analytics
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

> `docker-compose` 仅起基础设施；`box-server` / `box-web` 本地启动（见 `03-Architecture.md`）。

---

## PRD P0 / P1 / P2 对照（D-06）

| PRD 项 | 优先级 | 状态 | 模块 / 路由 |
|--------|--------|------|-------------|
| 用户注册 / 登录 | P0 | ✅ | `box-identity` · `/login` |
| Workspace | P0 | ✅ | `box-tenant` · 工作区切换 |
| Agent CRUD + Version | P0 | ✅ | `box-agent` · `/agents/:id/builder` |
| LLM 模型 | P0 | ✅ | `box-model` · `/models` |
| Conversation | P0 | ✅ | `box-conversation` · `/chat/:id` |
| Streaming Chat (SSE) | P0 | ✅ | `AgentChatExecutor` · `/chat` |
| Knowledge Base + RAG | P0 | ✅ | `box-knowledge` · `/knowledge` |
| Tool | P0 | ✅ | HTTP/DB/MCP/Function/Code 已通 |
| Workflow + Runtime | P0 | ✅ | `box-workflow` · Switch + 图校验 |
| Agent Runtime | P0 | ✅ | `box-agent` |
| Debug / Trace | P0 | ✅ | `/debug` · `/executions` |
| Publish + API Key | P0 | ✅ | Builder 发布 · `/settings/api-keys` |
| RBAC | P0 | ✅ | `box-security` · `/settings/roles` |
| 基础 Analytics | P0 | ✅ | `box-analytics` · `/analytics` |
| MCP | P1 | ✅ | `/mcp`（超前实现） |
| Long-term Memory | P1 | ✅ | Agent Builder Memory |
| Sub Agent / Webhook | P1 | ✅ | V18/V20 迁移 |
| Plugin / Marketplace | P1 | ✅ | `/plugin-market` · `/market` |
| Billing / Quota | P2 | ✅ | Settings Quota（超前实现） |
| Team Collaboration | P2 | ✅ | `/team` |
| Enterprise SSO | P2 | 🟡 | OAuth stub 已预留（B-17）；真实 IdP 对接待 backlog |

---

## API 抽检摘要（D-09）

| 域 | 路径前缀 | 状态 | 备注 |
|----|----------|------|------|
| 身份 | `/api/v1/auth/*` | ✅ | 注册/登录/忘记密码 + OAuth providers stub |
| 工作区 | `/api/v1/workspaces/*` | ✅ | |
| Agent | `/api/v1/agents/*` | ✅ | 含 chat SSE、工具/知识库绑定 |
| 对话 | `/api/v1/conversations/*` | ✅ | 含 stream/regenerate |
| 知识库 | `/api/v1/knowledge-bases/*` | ✅ | 含 test-answer、文档 retry |
| 工具/MCP | `/api/v1/tools/*`、`/mcp/*` | ✅ | |
| 工作流 | `/api/v1/workflows/*` | ✅ | 含 validate、debug、publish |
| 执行/Trace | `/api/v1/executions/*` | ✅ | |
| 分析 | `/api/v1/analytics/*` | ✅ | `box-analytics` 模块 |
| 审计 | `/api/v1/audit-logs` | ✅ | |
| 发布/API Key | `/api/v1/published/*`、`/api-keys/*` | ✅ | 含 embed-config 公开读取 |
| OAuth/SSO | `/api/v1/auth/oauth/*` | 🟡 | providers 列表 + authorize/callback stub（501） |

---

## 超前实现归档（D-07）

以下能力在 `02-PRD.md` 中多为 P1/P2，但代码已落地，不计入 `10-Gaps.md` 主清单：

| 能力 | 主要模块/路由 | 原 PRD 优先级 |
|------|----------------|---------------|
| MCP 管理 + Agent 绑定 + 运行时 | `box-tool` · `/mcp` | P1 |
| 长期记忆 | V17 · Agent Builder Memory | P1 |
| Sub Agent | V20 · `AgentSubAgentRuntimeService` | P1 |
| Webhook 节点 | V18 · `WebhookNodeExecutor` | P1 |
| 套餐 / 额度 / 账单 | `box-tenant` · Settings Quota/Billing | P2 |
| 插件市场 / Agent 市场 | `/plugin-market` · `/market` | P1 |
| Embed 对话 | `/embed/agents/:id` · 主题/Logo/欢迎语/推荐问题 | P1 |
| 忘记密码 | `/forgot-password` | P1 |
| 通知表与基础 API | V21 · `box-user` Notification | P1（UI/事件待完善 B-16） |

---

## 文档维护规范（D-11）

完成一项 B/F/D 任务或一个 Sprint 后：

1. 在 [`10-Gaps.md`](./10-Gaps.md) 将对应 ID 标记为 ✅
2. 在本文档追加/更新 Sprint 小节与 Phase 状态
3. 若涉及模块或 API 变更，同步 `03-Architecture.md` / `05-Backend.md` / `06-Runtime.md` 相关段落
4. 更新本文档「总览」完成度估算（避免使用「100%」）
5. 前端路由或验收路径变化时，更新「前端页面与路由」「V1 Demo 验收路径」

---

## 文档索引

1. `01-Prompt.md` — 总纲
2. `02-PRD.md` — 产品需求
3. `03-Architecture.md` — 架构
4. `04-Database.md` / `07-MySQL.md` — 数据库
5. `05-Backend.md` — 后端与 Phase 顺序
6. `06-Runtime.md` — Runtime 设计
7. `08-Name&slogan.md` — 品牌
8. **`09-Progress.md`** — 本文档（进度追踪）
9. **`10-Gaps.md`** — 未完成项可执行 TODO 清单
