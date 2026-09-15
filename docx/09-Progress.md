# Box V1 进度追踪

文档版本：V1.7 · 整体完成度：**~96%**

> 未完成项与执行顺序见 [`10-Gaps.md`](./10-Gaps.md)。本文档反映代码库真实状态，不再使用「100%」表述。

---

## 总览

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 文档规划 | ~96% | 2026-09-14 已补 Flyway V25–V29、B 端路由；PRD Super Admin 部分能力仍在 backlog |
| 后端实现 | ~96% | V1 主清单完成；OAuth 仍为 stub；B 端用户/审计/套餐知识库配额已接 |
| 前端实现 | ~95% | C 端 V1 完成；B 端工作台/用户/审计/主题已落地 |
| V1 可演示 | ~98% | **`10-Gaps.md` P0/P1/P2 主清单已全部完成** |

---

## Sprint 7 已完成（B 端运营补齐，2026-09-14）

| ID | 任务 | 状态 |
|----|------|------|
| A-01 | B 端工作台 `/dashboard`（统计 + 图表） | ✅ |
| A-02 | 全平台审计 `GET /api/v1/admin/audit-logs` | ✅ |
| A-03 | 平台用户列表启停 + `POST /api/v1/admin/users` 创建管理员 | ✅ |
| A-07 | 平台分析 `/analytics`（执行量 / Token / 租户用量） | ✅ |
| A-08 | 官方工具 `/platform-tools`、官方 MCP `/platform-mcp` | ✅ |
| A-04 | 套餐 `quotaKnowledgeBases` 读写 | ✅ |
| A-05 | 新建租户可选 PERSONAL / ENTERPRISE | ✅ |
| A-06 | 插件 `manifest_json`（Flyway V29） | ✅ |

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

## Flyway 迁移（当前 V1–V29）

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
| V25 | `V25__embed_custom_domain.sql` | Embed 自定义域名 |
| V26 | `V26__legal_documents.sql` | 法律文档配置 |
| V27 | `V27__column_comments.sql` | 列注释 |
| V28 | `V28__plugin_skills.sql` | 插件 Skill |
| V29 | `V29__plugin_catalog_manifest.sql` | 插件目录 manifest_json |

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
| 设置 · 个人信息 | `/settings/profile` | ✅ | |
| 设置 · 外观 | `/settings/appearance` | ✅ | |
| 设置 · 通用 | `/settings/general` | ✅ | |
| 设置 · 账号与安全 | `/settings/security` | ✅ | |
| 设置 · API 密钥 | `/settings/api-keys` | ✅ | 需 `api_key:manage` |
| 设置 · 角色与权限 | `/settings/roles` | ✅ | 需 `role:manage` |
| 设置 · 审计日志 | `/settings/audit-logs` | ✅ | 需 `audit:read` |
| 设置 · 额度管理 | `/settings/quota` | ✅ | `box-tenant` |
| 设置 · 账单概览 | `/settings/billing` | ✅ | `box-tenant` |
| 设置 · 容量管理 | `/settings/capacity` | ✅ | |
| 设置 · 关于 / 协议 | `/settings/about`、`/settings/legal` | ✅ | |
| 无权限 | `/forbidden` | ✅ | |
| 嵌入对话 | `/embed/agents/:id` | ✅ | 公开页 |
| 忘记密码 | `/forgot-password` | ✅ | 公开页 |

**管理后台 `box-admin-web`**（独立 Vite 应用，默认 `/dashboard`）：

| 页面 | 路由 | 备注 |
|------|------|------|
| 工作台 | `/dashboard` | 租户/资源统计与图表 |
| 平台分析 | `/analytics` | 执行量、成功率、Token、租户用量 TOP |
| 租户管理 | `/tenants` | 创建（含个人/企业）、启停、成员、套餐、额度 |
| 用户管理 | `/users` | 新建平台管理员、分页、类型/状态、启停 |
| 套餐管理 | `/plans` | 套餐与配额（含知识库上限） |
| 审计日志 | `/audit-logs` | 全平台操作记录 |
| 平台模型池 | `/platform-models` | 平台级模型与密钥 |
| 智能体市场 | `/agent-templates` | C 端模板上架 |
| 插件市场 | `/plugin-catalog` | 插件分类与上架 |
| 官方工具 | `/platform-tools` | 上架 HTTP 工具供租户安装 |
| 官方 MCP | `/platform-mcp` | 上架 MCP 服务供租户安装 |
| 运营位 | `/ops-placements` | C 端公告、推荐与市场精选 |
| 系统配置 | `/system-config` | 关于、协议、客服 |

已废弃或重定向：`/conversations`、`/chat/logs`、`/agents` → `/chat`

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

> **Docker Compose**（`deploy/docker-compose.yml`）：默认 `docker compose up -d` 起 MySQL / Redis / ES / MinIO；`docker compose --profile app up -d --build` 额外起 `box-server`（8080）。`box-web` / `box-admin-web` 仍本地 `npm run dev`（Vite 代理 `/api`）。

---

## 工程结构（已实现）

| 工程 | 说明 |
|------|------|
| `box-server` | 后端 Modular Monolith，`BoxApplication` 启动 |
| `box-web` | C 端 Vue 3 + TDesign，默认入口 `/chat` |
| `box-admin-web` | 平台管理端，租户/套餐/模板/插件 |
| `box-ui` | 共享 UI 组件与 Layout（`@box/ui`，供 web/admin 引用） |
| `deploy/` | `docker-compose.yml` + 可选 `app` profile |

### Workspace 与 Tenant 分工

| 概念 | 模块 / 表 | 用途 |
|------|-----------|------|
| **Workspace** | `box-workspace` · `workspace` / `workspace_member` | C 端工作区隔离；Agent / 知识库 / 工具等资源归属 |
| **Tenant** | `box-tenant` · `tenant` / `tenant_member` / `tenant_usage` | SaaS 租户、套餐、额度、账单（`/api/v1/billing`） |

C 端用户在工作区内操作；平台管理员在 `box-admin-web` 管理租户与套餐。

### 后端模块（`box-modules`）

`box-user` · `box-workspace` · `box-tenant` · `box-agent` · `box-model` · `box-knowledge` · `box-tool` · `box-workflow` · `box-conversation` · `box-runtime`（Workflow Runtime）· `box-publish` · `box-trace` · `box-analytics`

**Agent 对话 Runtime** 主实现位于 `box-agent`（`AgentChatExecutor` / `AgentChatPreparer`），非 `box-runtime`。

---

## PRD P0 / P1 / P2 对照（D-06）

| PRD 项 | 优先级 | 状态 | 模块 / 路由 |
|--------|--------|------|-------------|
| 用户注册 / 登录 | P0 | ✅ | `box-user` · `/login` |
| Workspace | P0 | ✅ | `box-workspace` · 工作区切换 |
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
| Billing / Quota | P2 | ✅ | `box-tenant` · `/settings/quota` · `/settings/billing` |
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
| 通知 | `/api/v1/notifications/*` | ✅ | 未读角标、已读；顶栏 `NotificationCenter` |
| 账单/额度 | `/api/v1/billing/*` | ✅ | `box-tenant` |
| 侧栏 | `/api/v1/sidebar` | ✅ | C 端菜单与工作区上下文 |
| 平台管理 | `/api/v1/admin/*` | ✅ | 租户、用户、套餐、审计、分析、运营位、平台模型、插件/工具/MCP、系统配置 |

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
| 通知中心 | V21 · `box-user` · `/notifications` · 顶栏 UI（B-16/F-18 ✅） | P1 |

---

## V1.5 商业化与运营（2026-09）

Flyway **V35–V37**；Phase A–E 按计划落地（支付网关、市场审核 UI、Model Router 接入 chat 仍 🟡）。

| 域 | 交付 | 状态 |
|----|------|------|
| 计费订阅 | Subscription / Invoice / PaymentRecord；C 端套餐商城 + mock 支付；B 端账单对账 | ✅ / 支付 🟡 |
| 超量策略 | `plan.overage_policy` + Quota REJECT/DEGRADE/METERED | ✅ |
| 邀请协作 | `workspace_invitation` + `/invite/{token}` + 待接受列表 | ✅ |
| 企业升级 | 个人→企业 `upgradeToEnterprise` | ✅ |
| 运营排障 | 用户上下文 Drawer；Analytics 租户下钻（不含代登录进 C 端） | ✅ |
| C 端体验 | 侧栏 IA；Debug/Executions 合并；对话导出；API 文档 Tab；Session 管理 | ✅ |
| B 端治理 | 平台角色创建；SMTP/OAuth 系统配置；管理端找回密码 | 🟡 RBAC 拦截待细化 |
| Phase E | 运营位埋点；知识库 URL 导入；市场审核字段；Model Router stub | 部分 🟡 |

**主要路由**：C 端 `/settings/plan`、`/team`、`/invite/:token`；B 端 `/billing-invoices`、`/forgot-password`。

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
