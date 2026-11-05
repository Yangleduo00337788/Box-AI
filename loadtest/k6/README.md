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

可选：`AGENT_ID`、`CONVERSATION_ID`、`KNOWLEDGE_BASE_ID`、`API_KEY`、`PUBLISHED_AGENT_ID`。不填时脚本会用列表接口的第一项，或为 chat 新建一条会话。

## 场景

| 脚本 | 覆盖 | 会打 LLM | 建议 |
|------|------|----------|------|
| `smoke` | 公开探活：`/system/health`、OAuth providers（`/health/details` 需登录，不纳入） | 否 | 每次发版先跑 |
| `auth` | `POST /auth/login` + `/auth/me` | 否 | 限流开启时 VU 要低 |
| `workspace` | 工作台读路径：Agent / 会话 / 知识库 / 通知 / 额度 | 否 | 上线主压测 |
| `mixed` | 按权重混合上述读接口 | 否 | 模拟控制台流量 |
| `chat` | `POST /conversations/{id}/messages`（`stream=false`） | 是 | 单独压，VU 低 |
| `rag` | `POST /knowledge-bases/{id}/search` | 检索/Embedding | 单独压 |
| `published` | `POST /published/agents/{id}/chat` + API Key | 是 | 开放调用容量 |

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

摘要会写到 `loadtest/k6/results/`（已 gitignore）。

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

压测请用**预发/隔离环境**，不要对生产真实用户和付费模型做 stress。Chat 使用非流式 JSON，便于看 p95；SSE 长连接不在本套脚本里。

## 建议顺序

1. `smoke`：进程与依赖探活  
2. `workspace` + `mixed`（`PROFILE=load`）：平台容量  
3. `auth` 小流量：登录是否被 Redis 限流打满  
4. `rag` / `chat` / `published`：检索与模型链路，VU 远低于读接口
