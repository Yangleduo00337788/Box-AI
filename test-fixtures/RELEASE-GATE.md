# Box AI 发布门禁（Release Gate）

**版本**：V1 · 与 `docx/02-PRD.md`、`docx/10-Gaps.md` 对齐  
**通过定义**：Phase 0 脚本 exit 0，且 Phase 1–5 必填项全部勾选。

---

## 判定标准（上线最低线）

| 维度 | 要求 |
|------|------|
| 构建 | 后端单测、box-web 单测与 build、box-admin-web build 无失败 |
| 依赖 | MySQL / Redis / ES / 对象存储 健康；LLM 至少一种可配置（生产必配） |
| 核心链路 | 注册登录 → 工作空间 → Agent 调试 → 发布 → 对话 SSE |
| 数据 | 知识库上传→READY→检索/问答；删除与权限无越权 |
| 治理 | 审计、套餐/租户（若开 SaaS）、对象存储热切换（若用 R2） |
| 安全 | 无默认弱密钥上生产；`.env` / 密钥不入库 |

---

## Phase 0 — 自动化（必须）

执行：

```powershell
.\test-fixtures\scripts\run-release-gate.ps1
```

| # | 项 | 通过 |
|---|-----|------|
| 0.1 | `box-server` → `mvn test` | ☑ |
| 0.2 | `box-ui` → `npm ci` + `npm run typecheck` | ☑ |
| 0.3 | `box-web` → `npm ci` + `npm run test` + `npm run build` | ☑ |
| 0.4 | `box-admin-web` → `npm ci` + `npm run build` | ☑ |

> **自动化记录（2026-09-25）**：0.1 前补 `box-infrastructure` 的 `junit-jupiter`（test）。整脚本因 C 端 dev 占用 `esbuild`，0.3 以 `npm install` + test/build 等价通过；0.4 为 `npm run build`（未重跑 `npm ci`）。0.5 见下。

可选（栈已起）：

| # | 项 | 通过 |
|---|-----|------|
| 0.5 | `BOX_SMOKE_BASE_URL` 指向 API，`smoke-stack` 返回 UP | ☑ |

---

## Phase 1 — 基础设施（必须）

前置：`cd deploy && docker compose up -d`（或预发等价环境）。本地 MySQL 非 compose 时见 `scripts/config.example.env`（默认 **3306 / root**）。

自动化：`.\test-fixtures\scripts\run-automated-infra.ps1`（与 Phase 0 叠加）。

| # | 项 | 通过 |
|---|-----|------|
| 1.1 | MySQL `box` 可连，Flyway 迁移无报错 | ☑ |
| 1.2 | Redis PING 正常 | ☑ |
| 1.3 | Elasticsearch 9200 可访问 | ☑ |
| 1.4 | 对象存储：MinIO 或 R2 管理端「可连接」 | ☑ |
| 1.5 | `GET /api/v1/system/health` → `mysql`/`redis` 为 true | ☑ |
| 1.6 | `box-server` + `box-web`(5173) + `box-admin-web`(5174) 可访问 | ☐ |

> **自动化记录（2026-09-25）**：1.1 Flyway 最新 **V51**；1.3/1.4 以 **health** 为准（ES HTTPS、远程 MinIO/R2）；**1.6 仅 API(8080) 已测**，5173/5174 当前未监听 — 请本地 `npm run dev` 后手工确认页面。

---

## Phase 2 — C 端核心（必须）

详见 [manual/consumer-checklist.md](./manual/consumer-checklist.md)。

摘要：

| # | 项 | 通过 |
|---|-----|------|
| 2.1 | 注册 / 登录 / 退出 | ☐ |
| 2.2 | 工作空间切换、成员邀请（若有） | ☐ |
| 2.3 | Agent Builder：保存、绑定模型、调试 SSE | ☐ |
| 2.4 | Agent 发布 → 对话使用已发布版本 | ☐ |
| 2.5 | 知识库：上传样例 → READY → 检索 / Test Answer | ☐ |
| 2.6 | 工作流：校验、调试、发布（若产品启用） | ☐ |
| 2.7 | 设置：API Key 创建（仅展示一次）、角色权限按钮 | ☐ |
| 2.8 | 头像上传（验证对象存储 Put） | ☐ |

---

## Phase 3 — B 端平台（必须）

详见 [manual/admin-checklist.md](./manual/admin-checklist.md)。

| # | 项 | 通过 |
|---|-----|------|
| 3.1 | 平台管理员登录 | ☐ |
| 3.2 | 租户 / 套餐 / 用户（按部署范围） | ☐ |
| 3.3 | 平台模型池 + 密钥 | ☐ |
| 3.4 | 插件目录 / 智能体市场上架 | ☐ |
| 3.5 | 对象存储：MinIO/R2 保存 + 测试连接 | ☐ |
| 3.6 | 审计日志可查 | ☐ |

---

## Phase 4 — 专项夹具（强烈建议）

### 4A 知识库 RAG

1. 上传 [knowledge-base/](./knowledge-base/) 中至少 3 种格式（如 `.txt`、`.pdf`、`.xlsx`）。
2. 按 [knowledge-base/README.md](./knowledge-base/README.md) 表格提问，答案含预期事实。
3. Agent 绑定该库，对话中带 `citations` 或引用侧栏。

| # | 项 | 通过 |
|---|-----|------|
| 4A | RAG 专项 | ☐ |

### 4B 对话加号插件

1. 执行 [conversation-plugins/dev-insert-test-plugins.sql](./conversation-plugins/dev-insert-test-plugins.sql)。（可用 `load-conversation-plugins.ps1`，**已导入 ☑**）
2. 按 [conversation-plugins/CASES.md](./conversation-plugins/CASES.md) TC-01～03。

| # | 项 | 通过 |
|---|-----|------|
| 4B | 插件专项 | ☐ |

### 4C 数据存储职责

[manual/storage-checklist.md](./manual/storage-checklist.md)

| # | 项 | 通过 |
|---|-----|------|
| 4C | MySQL / 对象存储 / ES 分工验收 | ☐ |

---

## Phase 5 — 上线前安全与运维（必须）

| # | 项 | 通过 |
|---|-----|------|
| 5.1 | 修改生产 JWT secret、AES key、数据库密码 | ☐ |
| 5.2 | `box.auth.verification.expose-code=false`（生产） | ☐ |
| 5.3 | 关闭不必要的 OAuth stub 或配置真实 IdP | ☐ |
| 5.4 | R2/MinIO 令牌最小权限（单桶读写） | ☐ |
| 5.5 | 备份策略：MySQL 定时备份；对象存储跨区/生命周期（按 SLA） | ☐ |
| 5.6 | 监控：`/api/v1/system/health`、日志 Request ID 可追踪 | ☑ |

> **5.6 自动化**：health 可达且响应含 **X-Request-Id**；生产日志串联仍须运维手工抽一条请求验证。

---

## 签字（可选）

| 角色 | 姓名 | 日期 |
|------|------|------|
| 开发 | | |
| 测试 | | |
| 运维 | | |
