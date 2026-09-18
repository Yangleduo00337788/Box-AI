# Box 未完成项 — 可执行 TODO 清单

文档版本：V1.1  
更新说明：2026-09-16 补平台 RBAC 细化与 `05-Backend.md` B 端路由抽检；主清单仍全部 ✅  
整体完成度估算：后端 ~96% · 前端 ~96% · **文档同步 ~96%**

---

## 使用说明

- **优先级**：P0（V1 必补）→ P1（体验/质量）→ P2（对齐 PRD 远期）
- **任务 ID**：`B-*` 后端 · `F-*` 前端 · `D-*` 文档
- **状态标记**：`⬜` 未开始 · `🟡` 进行中 · `✅` 已完成
- 完成某项后，在本文件勾选状态，并同步更新 `09-Progress.md`

---

## 一、后端

### P0 — V1 必补

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| B-01 | ✅ | **审计日志 Audit Log** | 新增 `audit_log` 表 + Flyway；记录登录、Agent CRUD、发布、知识库/工具/API Key、权限变更；提供 `GET /api/v1/audit-logs` 分页查询；写入带 userId、IP、action、resource、traceId |
| B-02 | ✅ | **Rerank 真正接入** | `KnowledgeSearchService` 在 `rerankEnabled=true` 时调用 Rerank 模型（非简单关键词打分）；支持绑定 `rerankModelId`；Agent RAG 链路生效 |
| B-03 | ✅ | **RBAC 统一校验** | 关键 API 统一走 `WorkspacePermissionService` 或 `@PreAuthorize`；补齐 agent/workflow/knowledge/tool 的 create/read/update/delete/publish 权限码覆盖 |
| B-04 | ✅ | **删除依赖检查** | Agent / Knowledge / Tool / Workflow 删除前检查绑定关系；已发布 Agent 删除返回明确冲突信息（409 + 依赖列表） |
| B-05 | ✅ | **Request ID 全链路** | 过滤器生成 `X-Request-Id`；日志、Execution、Trace 字段贯通 |
| B-06 | ✅ | **限流补齐** | 登录、Chat、API Key 调用、Tool 执行接入 Redis 限流；超限返回 429 |

### P1 — 质量与安全

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| B-07 | ✅ | **Workflow Switch 节点** | 新增 `SwitchNodeExecutor`；前端节点库可配置；Debug 可执行 |
| B-08 | ✅ | **Workflow 图校验** | `POST /workflows/{id}/validate` 检查：无 Start/Output、孤立节点、非法环、变量引用、Tool/Model/Knowledge 存在性 |
| B-09 | ✅ | **Code 节点沙箱加固** | `InlineScriptExecutor` 增加超时、禁止网络/文件 IO、内存上限；失败有明确错误码 |
| B-10 | ✅ | **文档处理状态机** | `knowledge_document.status` 细化：UPLOADING→PARSING→CHUNKING→EMBEDDING→INDEXING→READY/FAILED；失败写 `error_message` |
| B-11 | ✅ | **RAG Test Answer API** | `POST /knowledge-bases/{id}/test-answer`：检索 + LLM 生成 + 返回 citations |
| B-12 | ✅ | **流式 Tool Calling 事件** | SSE 补齐 `tool.start` / `tool.delta` / `tool.end`（对齐 `06-Runtime.md` §33） |
| B-13 | ✅ | **Function / Code Tool 运行时** | Agent 对话中 Function、Code 类型 Tool 可被执行（不仅 HTTP/DB/MCP） |
| B-14 | ✅ | **危险 Tool 执行确认** | Agent 工具绑定 `requireConfirmation`；SSE `tool.confirm` + Redis 令牌 + `POST /agents/{id}/tools/confirm` |
| B-15 | ✅ | **`box-analytics` 模块落地** | Analytics 已从 `box-conversation` 迁入 `box-analytics`；`box-bootstrap` 依赖已更新 |

### P2 — 架构对齐 / 远期

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| B-16 | ✅ | **通知中心后端完善** | 知识库文档完成/失败、工作流失败、Agent 发布、MCP 同步失败写入 notification |
| B-17 | ✅ | **OAuth / SSO 预留** | `GET /auth/oauth/providers` + GitHub/Google/SSO authorize/callback stub（501 + `box.oauth` 配置占位） |
| B-18 | ✅ | **docker-compose 加入 box-server** | `box-server/Dockerfile` + `deploy/docker-compose.yml` profile `app` 一键起全栈 |

---

## 二、前端

### P0 — V1 必补

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| F-01 | ✅ | **权限守卫** | 新增 `usePermission()` / 路由 `meta.permission`；创建/发布/删除等按钮按权限显隐；无权限跳转 403 页 |
| F-02 | ✅ | **恢复或明确 Agent 列表** | **已选方案 B**：保留 `/chat` 合并形态；`/agents` 重定向至 `/chat`；全站文案统一为「对话工作台」 |
| F-03 | ✅ | **删除依赖提示** | Agent/Knowledge/Tool/Workflow 删除弹窗展示依赖列表；已发布 Agent 二次确认 |
| F-04 | ✅ | **API Key 仅展示一次** | 创建/rotate 后仅弹窗显示完整 key；列表只显示 prefix；复制后不可再查看 |
| F-05 | ✅ | **知识库处理进度** | 文档列表展示状态步骤 + 失败原因 + 重试按钮 |

### P1 — 体验与 PRD 对齐

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| F-06 | ✅ | **Dashboard 入口策略** | 登录后默认 `/chat`；概览在头像菜单 Dialog，不以侧栏整页为主 |
| F-07 | ✅ | **Settings 信息架构** | `/settings/*` 承载 profile/api-keys/roles/audit 等；成员管理在 `/team`（`member:manage`）；无重复菜单 |
| F-08 | ✅ | **Analytics 图表** | 引入 ECharts；展示请求量、成功率、Token、延迟、Top Agents/Models（对接现有 API） |
| F-09 | ✅ | **Debug Console 三栏** | `/debug`：左 Input、中 Execution 树（Prompt→RAG→Tool→LLM）、右 Output；可展开每步 duration/tokens |
| F-10 | ✅ | **Chat 引用 Citation** | 消息内 `[1]` 可点击；侧边/弹窗展示原文 chunk |
| F-11 | ✅ | **Chat Trace 侧栏** | 对话页右侧可展开当前消息 Execution/Trace |
| F-12 | ✅ | **RAG 测试页** | 知识库详情：Search + Test Answer + 引用列表 |
| F-13 | ✅ | **危险 Tool 二次确认** | Builder/Chat 收到 `tool.confirm` 弹出确认 Dialog；绑定可勾选「需二次确认」 |
| F-14 | ✅ | **Workflow Switch 节点 UI** | 节点库 + 配置面板 + 多分支连线 |
| F-15 | ✅ | **全局 Empty / Error 态统一** | HTTP 拦截器记录 `X-Request-Id`；403 页展示 Request ID；429 错误码已对齐 |
| F-16 | ✅ | **审计日志页（管理端）** | C 端 `/settings/audit-logs`（`audit:read` 权限） |

### P2 — 远期 / 可选

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| F-17 | ✅ | **Embed Web Chat 定制** | Builder 发布 Tab 配置主题色/Logo/欢迎语/推荐问题；`GET/PUT /agents/{id}/embed-config`；公开 `GET /published/agents/{id}/embed-config` |
| F-18 | ✅ | **通知中心 UI** | 顶栏通知下拉 + 未读角标 + 60s 轮询 + 点击跳转 |
| F-19 | ✅ | **Agent 模板向导（C 端）** | `CreateAgentDialog` 支持「从模板创建」，对接 `GET /market/templates` + `POST /market/templates/{id}/enable` |
| F-20 | ✅ | **Workflow 编辑器 1920 优化** | 1920px 三栏加宽、画布/Minimap 高度优化 |

---

## 三、文档

### P0 — 与代码同步

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| D-01 | ✅ | **更新 `09-Progress.md`** | 完成度改为 ~85%；引用本清单；去掉不实「100%」 |
| D-02 | ✅ | **修正前端路由表** | 进度文档与 `05-Backend.md` §55 对齐实际路由（`/chat` 默认、`/agents` redirect 等） |
| D-03 | ✅ | **修正 Flyway 版本** | `09-Progress.md`、数据库文档改为 V1–V24（当前最新） |
| D-04 | ✅ | **修正模块结构说明** | `03-Architecture.md` / `05-Backend.md`：删除或标注 `box-api`；说明 Agent Runtime 在 `box-agent`；`box-analytics` 现状 |
| D-05 | ✅ | **修正 docker-compose 说明** | `03-Architecture.md` §51：默认 infra + 可选 `--profile app` 起 `box-server` |

### P1 — 分期与实现对照

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| D-06 | ✅ | **PRD P0/P1/P2 对照表** | 在 `09-Progress.md` 增加表格：每项 P0 标 ✅/🟡/❌ + 对应模块/路由 |
| D-07 | ✅ | **已实现超前项归档** | 见 `09-Progress.md`「超前实现归档」与本文档 §六 |
| D-08 | ✅ | **`06-Runtime.md` 与代码对齐** | 更新 Agent 执行类路径、`ChatStreamEvent` 事件列表、Tool 类型支持范围 |
| D-09 | ✅ | **API 清单抽检** | `05-Backend.md` 各 API 与 Controller 逐条核对，标注已实现/未实现/路径变更 |
| D-10 | ✅ | **验收路径更新** | `09-Progress.md`「V1 Demo 验收路径」已对齐 `/chat` 默认入口 |

### P2 — 维护机制

| ID | 状态 | 任务 | 验收标准 |
|----|------|------|----------|
| D-11 | ✅ | **进度更新规范** | 见 `09-Progress.md`「文档维护规范」 |
| D-12 | ✅ | **文档索引更新** | `09-Progress.md` 文档索引已含 `10-Gaps.md` 链接 |

---

## 四、建议执行顺序（4 个 Sprint）

### Sprint 1 — 闭环与安全 ✅

```
B-01 B-03 B-04 B-05  +  F-01 F-03 F-04  +  D-01 D-02 D-03
```

目标：审计、权限、删除安全、Request ID、文档进度诚实化。**已于 2026-09 完成。**

### Sprint 2 — RAG + 可观测 ✅

```
B-02 B-10 B-11 B-12  +  F-05 F-09 F-10 F-12  +  D-06 D-08
```

目标：Rerank、知识库状态、RAG 测试、调试与引用体验。**已于 2026-09 完成。**

### Sprint 3 — Workflow + 分析 ✅

```
B-07 B-08 B-09       +  F-08 F-14           +  D-04 D-09
```

目标：Switch 节点、图校验、Code 沙箱、Analytics 图表。**已于 2026-09 完成。**

### Sprint 4 — 体验收尾 ✅

```
B-06 B-13 B-14 B-16  +  F-02 F-06 F-07 F-11 F-15 F-16 F-13 F-18  +  D-05 D-07 D-10 D-12
```

### Sprint 5 — P1 收尾 ✅

```
B-15  +  D-11
```

目标：`box-analytics` 模块落地、文档维护规范。**已于 2026-09 完成。**

### Sprint 6 — P2 对齐 ✅

```
B-17 B-18  +  F-17 F-19 F-20
```

目标：OAuth stub、Docker 全栈 profile、Embed 定制、模板向导、Workflow 大屏布局。**已于 2026-09 完成。**

---

## 五、任务统计

| 分类 | P0 | P1 | P2 | 合计 |
|------|----|----|----|------|
| 后端 | 6 | 9 | 3 | **18** |
| 前端 | 5 | 11 | 4 | **20** |
| 文档 | 5 | 5 | 2 | **12** |
| **合计** | **16** | **25** | **9** | **50** |

> **P0 / P1 / P2 主清单已全部 ✅**（2026-09）。后续新需求请另开 backlog，勿与本清单混用。

---

## 六、已知：文档标为后续、代码已实现

以下能力在 `02-PRD.md` 中多为 P1/P2，但代码已超前落地，**不计入上方未完成清单**，仅需在文档中归档说明（见 D-07）：

| 能力 | 主要模块/路由 |
|------|----------------|
| MCP 管理 + Agent 绑定 + 运行时 | `box-tool` · `/mcp` |
| 长期记忆 | V17 迁移 · Agent Builder Memory |
| Sub Agent | V20 迁移 · `AgentSubAgentRuntimeService` |
| Webhook 节点 | V18 · `WebhookNodeExecutor` |
| 套餐 / 额度 / 账单 | `box-tenant` · Settings Quota/Billing |
| 插件市场 / Agent 市场 | `/plugin-market` · `/market` · `box-admin-web` |
| Embed 对话 | `/embed/agents/:id` · 主题/欢迎语/推荐问题可配置 |
| 忘记密码 | `/forgot-password` |

---

## 七、核心 Demo 已通 vs 仍缺摘要

### 已通（无需重复开发）

```
注册/登录 → Workspace → 创建 Agent → 配置 Prompt/模型
→ 上传知识库（RAG）→ 绑定 HTTP Tool → Builder SSE 调试
→ 发布 → Web Chat / Published API + API Key → Analytics / Execution Trace
```

### V1 主清单已清零

P0 / P1 / P2 共 50 项已全部完成。后续需求见下方 §八，勿与本清单混用。

---

## 八、对照 PRD 仍未做 / 未对齐（新 backlog）

主清单清零后，与 `02-PRD.md` Super Admin 及远期项仍有差距。以下**不是** V1 必补，按需排期。

| ID | 优先级 | 状态 | 任务 | 说明 |
|----|--------|------|------|------|
| BL-01 | P2 | 🟡 | 真实 OAuth / 企业 SSO | providers + authorize/callback **stub（501）**；身份表 V40 已预留，登录流待下一迭代 |
| BL-26 | P1 | ✅ | B 端市场灰度 UI | 模板/插件 `rollout_percent` + TENANT 白名单；`PUT .../rollout` |
| BL-27 | P1 | ✅ | C 端 Model Router | Builder `AUTO` + `routing_preference`；chat 运行时自动选模 |
| BL-28 | P1 | ✅ | C 端套餐 Mock 支付 | Settings 套餐订阅 + `confirmPayment`；真实 Stripe/Alipay 待下一迭代 |
| BL-02 | P2 | ✅ | B 端平台 Analytics | `/analytics` + `GET /api/v1/admin/analytics/overview|trends` |
| BL-03 | P2 | ✅ | B 端工具 / MCP 目录 | `/platform-tools`、`/platform-mcp`，复用插件目录 category |
| BL-04 | P2 | ✅ | Embed 自定义域名生效 | TXT / well-known + resolve + `box.embed.gateway-host` CNAME/TLS 指引 |
| BL-05 | P2 | ✅ | 计费支付闭环 | Stripe / Alipay / Mock 网关 + webhook/notify + C 端跳转支付 |
| BL-06 | P2 | ⬜ | Kubernetes / 分布式 Runtime | PRD §97 明确暂时不追求 |
| BL-07 | P1 | ✅ | 抽检 `05-Backend.md` B 端路由 | 已对齐 dashboard/users/audit 等管理端路由与 Admin API |
| BL-08 | P2 | ✅ | B 端创建平台管理员 | `POST /api/v1/admin/users` + 用户页弹窗 |
| BL-09 | P1 | ✅ | C 端套餐订阅 | Subscription + mock 支付确认 + Settings 套餐商城 |
| BL-10 | P1 | ✅ | 超量策略 overagePolicy | Plan `REJECT/DEGRADE/METERED` + Quota 执行 |
| BL-11 | P1 | ✅ | 邀请链接流 | `workspace_invitation` + `/invite/{token}` |
| BL-12 | P2 | ✅ | 个人→企业升级 | `POST /tenants/upgrade-enterprise` |
| BL-13 | P1 | ⬜ | B 端代登录 | 已移除（不采用代登录进 C 端） |
| BL-14 | P1 | ✅ | 用户上下文 Drawer | `GET /admin/users/{id}/context` |
| BL-15 | P1 | ✅ | Analytics 租户下钻 | `GET /admin/analytics/tenants/{id}` |
| BL-16 | P2 | ✅ | C 端侧栏 IA + Debug 合并 | 侧栏仅工作台三项；资源走插件市场/搜索；`/executions` → `/debug` |
| BL-17 | P2 | ✅ | 对话导出 Markdown | ChatView 导出按钮 |
| BL-18 | P2 | ✅ | API 文档 Tab | Settings API Keys OpenAPI/curl |
| BL-19 | P1 | ✅ | Session 设备管理 | `user_session` + `/auth/sessions` |
| BL-20 | P1 | ✅ | 平台 RBAC + 配置中心 | `platformAdminRole` JWT + Interceptor 路径矩阵；B 端菜单/路由显隐 |
| BL-21 | P2 | ✅ | 市场审核灰度 | `rollout_percent` + TENANT 白名单；C 端列表/安装按租户桶过滤 |
| BL-22 | P2 | ✅ | 运营位埋点 | `ops_placement_metric` + track API + C 端上报 |
| BL-23 | P2 | ✅ | Auto 智能路由 | `model_source=AUTO` + `routing_preference`；chat 接入 Model Router |
| BL-24 | P2 | ✅ | 知识库 URL 导入 | `import-url` API + KnowledgeView UI |
| BL-25 | P1 | ✅ | 统一其余对话 UI | Builder 调试预览 + Embed 页对齐 `/chat` 的 TDesign Chat |
| E2E-01 | P1 | ✅ | C 端未知路由 404 | R15：`NotFoundView` + catch-all；设置子路由同步 |
| E2E-02 | P1 | ✅ | 受邀跨租户工作空间被 hydrate 冲掉 | R14：`applyWorkspaceSelection` 保留本地 workspaceId |
| E2E-03 | P1 | ✅ | 插件目录缺上架/下架行操作 | R5：对齐模板市场，审核通过后可直接上/下架 |
| E2E-04 | P1 | ✅ | 市场列出不可运行模板 | R2：`listMarket` 过滤 `isRunnable`；启用失败文案明确 |
| E2E-05 | P2 | ✅ | B 端用户关键字搜索参数绑定 | R11：`email/nickname/username` 分别绑定 like 参数 |

**已对齐、文档曾滞后（现已写入 `09-Progress.md`）**：Flyway V25–V29；B 端工作台/用户/审计/租户类型/知识库配额；C 端默认入口 `/chat`（PRD 写 Dashboard 首页，以 F-06 产品决策为准）；对话 UI 三处统一见 Sprint 8。

---

## 文档索引

1. `01-Prompt.md` — 总纲
2. `02-PRD.md` — 产品需求
3. `03-Architecture.md` — 架构
4. `04-Database.md` / `07-MySQL.md` — 数据库
5. `05-Backend.md` — 后端与 Phase 顺序
6. `06-Runtime.md` — Runtime 设计
7. `08-Name&slogan.md` — 品牌
8. `09-Progress.md` — 进度追踪
9. **`10-Gaps.md`** — 本文档（未完成项 TODO）
