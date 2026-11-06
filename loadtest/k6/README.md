# Box k6 压测

上线前用 [k6](https://k6.io/) 压 `box-server`。对话 / RAG / 开放 API 会打到模型和向量检索，默认与平台读接口拆开，避免把 LLM 额度打爆。

## 安装

```bash
# Windows (winget / chocolatey / scoop 任选)
winget install Grafana.k6
```

确认 `k6 version` 可用。目标环境需要已启动的 `box-server`（本地默认 `http://localhost:8080`）。

## 配置

```bash
cp loadtest/k6/env.example loadtest/k6/.env
```

必填（除 `smoke` 外）：`ACCOUNT`、`PASSWORD`。企业登录再填 `ACCOUNT_TYPE=ENTERPRISE` 和 `ORG_ID`。

可选：`AGENT_ID`、`CONVERSATION_ID`、`KNOWLEDGE_BASE_ID`、`API_KEY`、`PUBLISHED_AGENT_ID`、`WORKFLOW_ID`。不填时脚本会用列表接口的第一项，或为 chat / workflow 新建资源。

## 场景

| 脚本 | 覆盖 | 会打 LLM | 建议 |
|------|------|----------|------|
| `smoke` | 公开探活：`/system/health`、OAuth providers（`/health/details` 需登录，不纳入） | 否 | 每次发版先跑 |
| `auth` | `POST /auth/login` + `/auth/me` | 否 | 限流开启时 VU 要低 |
| `workspace` | 工作台读路径：Agent / 会话 / 知识库 / 通知 / 额度 | 否 | 上线主压测 |
| `mixed` | 按权重混合上述读接口 | 否 | 模拟控制台流量 |
| `chat` | `POST /conversations/{id}/messages`（`stream=false`） | 是 | 单独压，VU 低 |
| `chat-sse` | 同上，`stream=true`（工作台主路径；每 VU 独立会话） | 是 | 测长连接/准入，VU 勿上到读路径量级 |
| `rag` | `POST /knowledge-bases/{id}/search` | 检索/Embedding | 单独压 |
| `published` | `POST /published/agents/{id}/chat` + API Key | 是 | 开放调用容量 |
| `workflow` | `POST /runtime/workflows/{id}/execute`（Start→Knowledge→LLM→Output） | 是 | 完整图；VU 勿上到读路径量级 |
| `embed` | 公开 `GET embed-config` + Embed SSE Chat（API Key） | 是 | 对齐公开页；VU 勿上到读路径量级 |
| `chat-tool` | 绑定 HTTP Tool 的 SSE 对话（`tool.start`/`tool.end`） | 是 | VU 低于纯对话 |
| `chat-mcp` | 绑定 MCP 的 SSE 对话（公开 Streamable HTTP） | 是 | VU 低于纯对话；本机 SSRF 禁 localhost |
| `register` | 发注册验证码 + `POST /auth/register`（独立邮箱） | 否 | 需 `BOX_AUTH_VERIFICATION_EXPOSE_CODE=true` |

`PROFILE`：

| 值 | 行为 |
|----|------|
| `smoke`（默认） | 2 VU × 30s |
| `load` | 20 → 50 VU，约 5 分钟 |
| `stress` | 50 → 200 VU，约 6 分钟 |
| `heavy` | 20s 拉到 1000 VU，稳住 2 分钟 |

也可覆盖：`.\run.ps1 -Scenario mixed -Vus 1000 -Duration 2m`。

默认阈值：读接口失败率低于 1%、p95 低于 800ms；登录/LLM 更宽松（失败率低于 5%，LLM p95 低于 30s）。

## 运行

Windows：

```powershell
cd loadtest/k6
.\run.ps1 -Scenario smoke
.\run.ps1 -Scenario mixed -Profile load
.\run.ps1 -Scenario chat -Profile smoke
.\run.ps1 -Scenario chat-sse -Vus 200 -Duration 2m
```

Linux / macOS：

```bash
cd loadtest/k6
chmod +x run.sh
./run.sh smoke
PROFILE=load ./run.sh mixed
PROFILE=smoke ./run.sh chat
```

或直接：

```bash
k6 run -e BASE_URL=http://localhost:8080 -e ACCOUNT=you@example.com -e PASSWORD=secret \
  -e PROFILE=load loadtest/k6/scenarios/mixed.js
```

摘要会写到 `loadtest/k6/results/`（已 gitignore）。结论报告在 [`docx/loadtest/`](../../docx/loadtest/)，数字总表 [`SUMMARY.md`](../../docx/loadtest/SUMMARY.md)。

**已跑（本机 `dev`，2026-11）**：`smoke` / `workspace` / `mixed`（含 1000 VU）、`register`、`auth`、`chat` 非流式、`chat-sse`、`rag`、`published` 200 VU、`workflow` 200 VU、`embed` 200 VU、`chat-tool`。工作台对话以 `chat-sse` 200 VU 为准。

主机采样（Redis / MySQL / jstat / 线程栈 / JFR），与 k6 **并行**另开窗口：

```powershell
.\sample-host.ps1 -IntervalSec 5 -DumpEvery 3 -DurationSec 180
```

线程 dump 不要每个 tick 都打，`Get-NetTCPConnection` 在高连接数下会拖到几十秒，采样本身会污染结果。

## 后端限流（不要误判为压垮）

限流在 **box-server** 的 Redis 里，不在 `loadtest/k6/.env`。默认：

| 范围 | 上限 | 环境变量 |
|------|------|----------|
| 登录 | 20 次 / IP / 分钟 | `BOX_RATE_LIMIT_LOGIN_PER_MINUTE` |
| 控制台 Chat | 60 次 / 工作空间用户 / 分钟 | `BOX_RATE_LIMIT_CHAT_PER_MINUTE` |
| 已发布 Chat | 60 次 / Agent+IP / 分钟 | `BOX_RATE_LIMIT_PUBLISHED_CHAT_PER_MINUTE` |
| API Key | 120 次 / Key / 分钟 | `BOX_RATE_LIMIT_API_KEY_PER_MINUTE` |

压测改仓库根 `.env` 后**重启** `box-server`：

```
BOX_RATE_LIMIT_ENABLED=false
BOX_QUOTA_ENABLED=false
# 测对话极限容量时可关：BOX_CHAT_ADMISSION_ENABLED=false
```

或只把某个 per-minute 调大（`0` 表示该范围不限）。生产不要关限流、配额或对话准入。

压测请用**预发/隔离环境**，不要对生产真实用户和付费模型做 stress。非流式看 JSON p95；工作台主路径用 `chat-sse`（k6 等到 SSE `done` 才结束该次请求）。

## 建议顺序

1. `smoke`：进程与依赖探活  
2. `workspace` + `mixed`（`PROFILE=load`）：平台容量  
3. `auth` 小流量：登录是否被 Redis 限流打满  
4. `register`：并发建账号（先开 `expose-code`）  
5. `chat-sse`（工作台主路径）再 `chat` 非流式对照；VU 以准入 `maxInflight`（默认 200）为上限，不要上到读路径 1000  
6. `rag`：检索已跑 200 VU（通过）  
7. `published`：200 VU 已通过（`qwen-flash`）

8. `workflow`：200 VU 已通过（仅 `qwen3.8-27b`）
9. `embed`：200 VU 已通过（公开配置 + SSE）
10. `chat-tool`：Flash 50 VU 通过；27b 同规模未过  
11. `chat-mcp`：1 VU 通；50 VU HTTP 通、SSE checks 未过（公网 MCP）
