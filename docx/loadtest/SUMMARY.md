# Box 压测数据汇总

本机 `http://localhost:8080`，Spring `dev`，k6 v2.3.0。限流/配额在对话与登录大并发轮次中关闭（`BOX_RATE_LIMIT_ENABLED=false`，`BOX_QUOTA_ENABLED=false`）。对话准入保持默认（`maxInflight=200`）。不含账号口令或 JWT。

分报告在本目录各篇；本页只收数字与结论。脚本说明见 [`loadtest/k6/README.md`](../../loadtest/k6/README.md)。

## 判定口径

| 结果 | 含义 |
|------|------|
| 工作台以 **SSE 200 VU 通过** 为准 | 产品主路径；阈值全过 |
| 读路径 1000 VU 阈值通过 | 开闸约 0.8% 拒连，随后恢复；不算事故 |
| 登录 180 VU 业务通过 | 失败全是开闸 1～2 秒拒连；生产有 20 次/IP/分钟，不修 |
| 注册修后正确性通过 | p95 1.84s 是 bcrypt+建账号，不是 500 |
| 非流式 200 VU **不作为上线否决** | 同步事务占连接池；工作台不走这条 |

本机 `dev`、无网关，**不能写成生产 SLA**。生产仍应开限流、配额、对话准入。

正式归档以本表 + `docx/loadtest/` 分报告为准。`loadtest/k6/results/` 里还有 11-05 登录/注册/非流式试跑（限流仍开着、数字不升格）以及 11-04 晚间重复 mixed/chat，**不写入结论**。

## 总表

| 日期 | 场景 | 规模 | 请求/迭代 | 失败率 | p95 | 结论 |
|------|------|------|-----------|--------|-----|------|
| 11-04 | `smoke` 探活 | 2 VU × 30s | 120 req | **0%** | 2.5ms | 通过 |
| 11-04 | `workspace` 工作台读 | 2 VU × 30s | 1,761 req | **0%** | 6.8ms | 通过 |
| 11-04 | `mixed` 读路径 | 50 VU load ≈ 5 分钟 | 66,959 req | **0%** | 6.3ms | 通过 |
| 11-04 18:39 | `mixed` | **1000 VU × 2 分钟** | 417,208 req | **0.75%** | **283ms** | 阈值过；开闸拒连 |
| 11-04 20:00 | `mixed` 无采样对照 | **1000 VU × 2 分钟** | 412,461 req | **0.77%** | **291ms** | 推荐对照；与 18:39 一致 |
| 11-04 18:50 | `mixed` 带主机采样 | 1000 VU × 2 分钟 | — | **7.79%** | 污染 | 不当对照；瓶颈 Tomcat 200 线程 |
| 11-06 16:04 | `register` 修前 | 180 VU × 1 分钟 | 6,841 req | 2.22% | 2.32s | 149 次注册死锁 500 |
| 11-06 16:31 | `register` 修后 | 180 VU × 1 分钟 | 8,285 req | **0.13%** | 1.84s | 注册全成功；p95 阈值未过 |
| 11-06 16:51 | `auth` 登录 | 2 VU × 30s | 112 req | **0%** | 79ms | 通过 |
| 11-06 16:51 | `auth` 登录 | **180 VU × 1 分钟** | 12,559 req | **6.25%** | **1.08s** | 业务全成功；失败全是开闸 1～2 秒拒连 |
| 11-06 17:22 | `chat` 非流式 | 1 VU × 20s | 6 次发消息 | **0%** | 4.6s | 通过 |
| 11-06 17:23 | `chat` 非流式 | **200 VU × 2 分钟**（准入上限） | 713 次发消息 | **91.9%** | **62s** | 单路通；200 路排队失败 |
| 11-06 17:34 | `chat-sse` | 1 VU × 20s | 6 次 SSE | **0%** | 3.6s | 通过 |
| 11-06 17:34 | `chat-sse` | **200 VU × 2 分钟** | 7,832 次 SSE | **0%** HTTP；done **99.6%** | **4.9s** | 阈值全过；工作台主路径 |
| 11-06 17:42 | `rag` | setup 失败 | 2 req | 脚本中止 | — | 压测号当时无知识库；建库 56 后重跑 |
| 11-06 17:43 | `rag` | 1 VU × 20s | 8 次检索 | **0%** | 2.21s | 通过 |
| 11-06 17:43 | `rag` | **200 VU × 2 分钟** | 8,919 次检索 | **0%** | **2.33s** | 阈值全过；延迟在 Embedding |
| 11-06 17:50 | `published` | 1 VU × 20s | 16 次开放 Chat | **100%** | 363ms | 鉴权通；`qwen3.8-omni-flash` 额度耗尽 16001 |
| 11-06 18:24 | `published` | 1 VU × 20s | 17 次开放 Chat | **100%** | ~230ms | 改绑 `qwen-mt-uni` 仍 16001（翻译接口，不是 Chat） |
| 11-06 18:27 | `published` | **200 VU × 2 分钟** | 16,714 次开放 Chat | **0.28%** | **576ms** | 阈值全过；换 `qwen-flash`；开闸 47 次拒连 |
| 11-06 18:46 | `workflow` | 1 VU × 20s | 6 次完整图执行 | HTTP **0%**；业务 **100%** FAILED | 2.55s | 欠费时 LLM 失败 |
| 11-06 19:25 | `workflow` | 1 VU × 20s | 2 次完整图 | **0%** | 13.0s | 仅 `qwen3.8-27b`；`SUCCEEDED` |
| 11-06 19:25 | `workflow` | **200 VU × 2 分钟** | 2,678 次完整图 | **0%** | **15.7s** | 阈值全过；2678/2678 `SUCCEEDED` |
| 11-06 19:33 | `embed` | 1 VU × 20s | 15 轮（配置+SSE） | **0%** | 521ms | 通过 |
| 11-06 19:34 | `embed` | **200 VU × 2 分钟** | 16,709 轮 / 33,419 HTTP | **0%** | **543ms** | 阈值全过；SSE `done` 100% |
| 11-06 19:38 | `chat-tool` | 1 VU × 20s | 1 次 SSE+工具 | **0%** | 8.2s | `done`+`tool.start/end` |
| 11-06 19:39 | `chat-tool` | **50 VU × 2 分钟** | 516 次 | HTTP **0%** | **21.9s** | checks 85%；`done` 73%；tool 88% |
| 11-06 19:41 | `chat-tool` | 20 VU × 2 分钟 | 1,972 次 | HTTP **0%** | 275ms | 50 路之后立刻跑；0 `done`（模型 error） |
| 11-06 19:48 | `chat-tool` | 1 VU × 20s | 2 次 | **0%** | ~10s | **`qwen3.8-flash`**；`done`+工具事件 |
| 11-06 19:49 | `chat-tool` | **50 VU × 2 分钟** | 707 次 | **0%** | **11.9s** | **Flash 阈值全过**；`done` 706/707；tool 99% |
| 11-06 19:51 | `workflow` | 1 VU × 20s | 4 次 | **0%** | 5.1s | Flash；3/4 `SUCCEEDED` |
| 11-06 19:54 | `workflow` | **200 VU × 2 分钟** | 7,097 次 | HTTP 1.28% | 2.59s | Flash；**0/7097 `SUCCEEDED`**（免费额度） |
| 11-06 20:02 | `workflow` | 1 VU × 20s | 3 次完整图 | **0%** | 7.8s | **`qwen3.8-max-0902`**；3/3 `SUCCEEDED` |
| 11-06 20:03 | `workflow` | **200 VU × 2 分钟** | 3,056 次完整图 | **0%** | **9.83s** | 阈值全过；3056/3056 `SUCCEEDED` |
| 11-06 20:13 | `chat-mcp` | 1 VU × 20s | 2 次 SSE+MCP | **0%** | 9.9s | `done`+`tool.start/end` |
| 11-06 20:14 | `chat-mcp` | **50 VU × 2 分钟** | 1,200 次 | HTTP **0%** | **19.8s** | checks 39%；`done` 21%；公网 MCP |

## 读路径（11-04 mixed 1000 VU，18:39）

吞吐约 **3457 req/s**，平均 152ms，最大 1.12s。失败是开闸 `connection refused`，随后恢复。压测号当时无 Agent/会话，未打详情接口。

无采样 20:00 一轮：3416 req/s，失败 0.77%，p95 291ms，p99 385ms。

## 注册（11-06，180 VU × 1 分钟）

每轮：`POST /auth/verification-code` → `POST /auth/register`。

| | 修前 | 修后 |
|---|---|---|
| 迭代 | 3,422 | 4,148 |
| 吞吐 | 45 iter/s | 63 iter/s |
| 注册 | 3,270 成功 / 149 失败 | **4,137 / 4,137** |
| 死锁 | 149 | **0** |
| p95 / p99 / 最大 | 2.32s / 3.30s / 16.5s | 1.84s / 3.78s / 16.9s |

修后 11 次失败是开闸拒连。p95 是 bcrypt + 建账号，不是业务错误。

## 登录（11-06，180 VU × 1 分钟）

每轮：`POST /auth/login` → `GET /auth/me`。登录 5887 成功 / 785 失败；`/auth/me` **5887 / 5887**。785 次全部是 `16:51:49`–`16:51:50` 的 TCP 拒连，之后约 58 秒 0 拒连。p95 1.08s 过阈值；失败率 6.25% 未过。不算功能缺陷。

## 非流式 Chat（11-06，准入 200）

`POST /conversations/{id}/messages`，`stream=false`，Agent 47 / 会话 57。

| | 1 VU | 200 VU |
|---|---|---|
| 成功 / 失败 | 6 / 0 | 57 / 656 |
| checks | 100% | 8.1% |
| p95 | 4.6s | 62s |
| 库里成功 execution | — | 64 条，平均 **2.5s**（0.9～11s） |

连得上，不是拒连。打到模型的很快；失败是排队（非流式事务占着线程和连接池）。不必当事故修；主路径是 SSE。

## SSE Chat（11-06，准入 200）

`POST /conversations/{id}/messages`，`stream=true`，每 VU 独立会话。HTTP 8,033，失败 0%。SSE `done` 7803 / 7832；29 次 60s 级未完成（库里 29 条 RUNNING）。成功 execution 平均 2.1s。k6 退出 0。

非流式在 `@Transactional` 里同步等 LLM，占着 Hikari（默认约 10）和 Tomcat 工作线程；SSE 先提交用户消息再在虚拟线程里调模型，所以同为 200 路时只有非流式垮。

## RAG 检索（11-06，200 VU）

第一次 `rag-20261106-174227.json` 因无知识库中止。建库 56 后：`POST /knowledge-bases/56/search`，HYBRID。8,919 次全部成功，p95 2.33s（单路 2.21s），吞吐 65 req/s。最大 24.6s 为个别慢 embedding。k6 退出 0。

## 开放 API（11-06，200 VU）

`POST /published/agents/47/chat` + `ax_live_*`。17:50 一轮 16 次全部 `code=16001`（`qwen3.8-omni-flash` 免费额度耗尽）。18:24 改绑 `qwen-mt-uni` 仍 17/17 失败（翻译接口）。换 **`qwen-flash`** 后 18:27 一轮：16,714 次，失败 47（0.28%，开闸拒连），p95 576ms，k6 退出 0。

`qwen-mt-uni` 有额度但不是 Chat Completions（翻译 `source_texts`），不能绑对话 Agent。开放 API 非流式 200 路能过，因为已发布路径不把整轮 LLM 包在会话事务里。

## 完整工作流（11-06，200 VU）

`POST /runtime/workflows/30/execute`，图 Start→Knowledge→LLM→Output，仅 **`qwen3.8-27b`**。缴费后 200 VU：2,678 次全部 `SUCCEEDED`，HTTP 失败 0%，p95 15.7s，k6 退出 0。

## Embed 公开页（11-06，200 VU）

对齐 `/embed/agents/47`：匿名 `GET embed-config` + API Key SSE Chat。16,709 轮全部成功，HTTP 33,419，失败 0%，checks 100%，p95 543ms，k6 退出 0。未压自定义域名 resolve。

## HTTP Tool SSE（11-06）

Agent `k6-tool-agent` + HTTP `k6_ping`，仅 `qwen3.8-27b`。1 VU 打出 `tool.start/end`。50 VU：HTTP 0 失败，SSE `done` 377/516，工具事件 455/516，checks 未过。紧随 20 VU 全部 SSE error，不当对照。MCP 见后文。

## `qwen3.8-flash`（11-06 19:48）

平台模型 113（MaaS）/ 114（DashScope）。脚本默认 `PLATFORM_MODEL_CODE=qwen3.8-flash`。

- **Tool 50 VU 通过**：707 轮，HTTP 0 失败，SSE `done` 706/707，工具事件 699/707，p95 11.9s，k6 退出 0。比 27b 同规模明显稳。
- **工作流 200 VU 未过**：HTTP 失败 1.28%，**0 / 7097 `SUCCEEDED`**，p95 2.59s（失败返回）。阿里云 `insufficient_quota`。

## `qwen3.8-max-0902` 工作流（11-06 20:03）

换 Max 后 200 VU：3,056 次全部 `SUCCEEDED`，HTTP 失败 0%，p95 9.83s，k6 退出 0。工作流容量与 27b 那轮一并成立。

## MCP SSE（11-06 20:13）

公开 `aisenseapi.com/mcp` + `k6-mcp-agent` + `qwen3.8-max-0902`。1 VU 通。50 VU HTTP 0 失败，SSE `done` 251/1200，库里约 949 条仍 `RUNNING`。公网 MCP + 多轮 tool-call，不当引擎否决。本机 SSRF 不能打 localhost mock。

## 本轮归档（2026-11-06 告一段落）

规划场景（LT-01～06）数字均已写入本表与 `docx/loadtest/` 分报告。k6 原始摘要在 `loadtest/k6/results/`（gitignore，不入库）。

未纳入、不挡本轮结论：多机、反向代理、HTTPS、生产 JVM、打开限流/配额、更复杂工作流图、Embed 自定义域名、自建 MCP 高并发。

## 环境共性

- MySQL 8.0 `box` :3306，WSL Redis `127.0.0.1:6379`
- 读/登录/注册压测号 `loadtest@box.local`；注册另用 `k6reg.*@box.local`
