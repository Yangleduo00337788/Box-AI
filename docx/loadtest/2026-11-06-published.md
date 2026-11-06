# Box 压测报告：开放 API `published`（未完成 200 VU）

| 项 | 内容 |
|----|------|
| 日期 | 2026-11-06 17:50–17:52（UTC+8） |
| 产品 | Box |
| 目标 | 本机 `http://localhost:8080`，`dev` |
| 脚本 | `loadtest/k6/scenarios/published.js`（`stream: false` + API Key） |
| 命令 | `.\run.ps1 -Scenario published -Vus 1 -Duration 20s` |
| 摘要 | `loadtest/k6/results/published-20261106-175044.json` |

---

## 1. 结论

**开放 API 鉴权通了，模型调用当前失败，因此没有打 200 路。**

1 VU × 20s：16 次 `POST /api/v1/published/agents/47/chat` **全部 HTTP 失败**，业务码 **16001「模型调用失败」**，p95 **363ms**（没有真正等到 LLM 生成）。k6 退出 99。

同一 Agent 用 JWT 打控制台 `POST /conversations/{id}/messages` 也是 16001。说明不是 API Key 配错，而是 **该 Agent 绑定的平台模型此刻调不通**（`platform_model` id 102，`qwen3.8-omni-flash`）。下午 SSE 200 路还成功过，更像供应商限流/额度，而不是开放 API 代码没接通。

未跑 200 VU，避免把「模型 400」写成开放 API 容量。

---

## 2. 已准备的压测资源

| 项 | 值 |
|----|------|
| Agent | 47 `k6-ai-ping`，已发布 version 73 |
| 鉴权 | `Authorization: Bearer ax_live_…`（完整 Key 只在本地 `loadtest/k6/.env`，不入库） |
| 工作空间 | 32 |
| 限流 | `BOX_RATE_LIMIT_ENABLED=false`（否则 Key 120/分、已发布 Chat 60/分） |

curl 非流式返回 HTTP 400 + `code=16001`；`stream: true` 返回 HTTP 200 SSE，`type=error`，同样是「模型调用失败」。库里 `execution` 为 FAILED，耗时约 180–270ms。

---

## 3. 建议

200 VU 已在换模型后补完，见 [2026-11-06-published-200vu.md](./2026-11-06-published-200vu.md)。

- 开放 API 也支持 SSE；若要对照工作台流式，可再补 `stream: true`。
- 现网 Key 仍应开 120/分 与已发布 Chat 60/分。
