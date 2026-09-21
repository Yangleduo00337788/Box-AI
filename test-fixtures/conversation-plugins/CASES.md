# 对话加号插件 — 测试用例（发送内容 & 预期）

前提见 [README.md](./README.md)。下列「示例回答」为**验收参考**（模型措辞可能不同，以**结构/事件**为准）。

---

## 界面预期（2026-03 起）

- 用户消息**不再**在正文末尾拼接「本次消息启用的插件」；启用项以气泡下方**可点击标签**展示（数据来自 `metadataJson.plugins`）。
- 助手回复时：调用 HTTP/MCP 工具会出现与「正在思考中」同款的**流光文案**（如「正在调用 工具 · …」）；结束后在消息末尾显示**可点击**的调用记录标签。
- Skill 无 tool 事件：用户消息显示技能标签；助手消息末尾显示「已调用 技能 · …」标签（若上一条用户消息启用了技能）。

---

## 通用前置（每条用例）

| 步骤 | 操作 |
|------|------|
| 1 | 已执行 `dev-insert-test-plugins.sql`（或后台手动上架同 manifest 的插件） |
| 2 | 插件市场 → 安装对应 `[测试]` 插件 |
| 3 | 打开任意**已配置模型**的智能体对话 |
| 4 | 输入框 **加号 → 插件**，勾选**仅 1 个**待测插件（不要混选知识库/工作流） |
| 5 | 发送下表「用户消息」 |

**不应出现**：选择 `knowledge` / `workflows` 类插件仍能进加号列表（应被前端过滤或后端 400）。

---

## TC-01 Skill（三条要点）

| 项 | 内容 |
|----|------|
| 插件 | `[测试] 三条要点 Skill`（`fixture-chat-skill-brief`） |
| 用户消息 | `用一句话说明 Box AI 是什么` |
| 预期现象 | 用户消息 metadata 含该插件；**不要求**出现 toolStart |
| 预期回答结构 | 第 1 行含 `【技能已启用】`；随后**恰好 3 行**以 `•` 开头的要点 |
| 示例回答 | 见下方引用块 |

```text
【技能已启用】
• Box AI 是把模型、知识库、工具与工作流装在同一工作空间里的智能体平台。
• 你可以创建 Agent、绑定知识库，并在对话里按需启用技能或工具。
• 适合团队做问答助手、流程自动化与可复用的插件能力。
```

**失败判定**：没有 `【技能已启用】`；要点不是 3 条；或模型去调了 HTTP/MCP 工具（本技能 manifest 已要求不要调工具）。

---

## TC-02 Tool（HTTPBin GET）

| 项 | 内容 |
|----|------|
| 插件 | `[测试] HTTPBin GET`（`fixture-chat-tool-httpbin`） |
| 用户消息 | `请调用当前可用的 HTTP 工具，用 GET 请求，查询参数 name=box-plugin-test，把工具返回的 JSON 原文贴出来。` |
| 预期现象 | 流式/SSE 或调试里出现 **toolStart / toolEnd**；`toolKey` 类似 `plugin-fixture-chat-tool-httpbin` |
| 预期工具结果要点 | JSON 中含 `"args"` 且 `"name": "box-plugin-test"`（httpbin 标准响应） |
| 示例助手总结 | `工具返回中 args.name 为 box-plugin-test，说明对话加号注入的 HTTP 工具已执行。` |

**失败判定**：无 tool 事件；或报错「未找到工具」「工具不可用」「插件未安装」。

**说明**：智能体若在编排里绑定了大量其他工具，可能抢答；建议用**未绑 HTTP 工具**的 Agent，仅依赖加号插件。

---

## TC-03 MCP（Echo，需本机 MCP）

| 项 | 内容 |
|----|------|
| 插件 | `[测试] MCP Echo`（`fixture-chat-mcp-echo`） |
| 用户消息 | `请调用 MCP 工具 echo，参数 message=hello-box-mcp，并告诉我工具返回内容。` |
| 预期现象 | 出现 tool 事件；`toolKey` 形如 `mcp_plugin_fixture_chat_mcp_echo_echo`（以 serverKey 为准） |
| 预期成功 | toolEnd 状态成功，输出中含 `hello-box-mcp` 或 MCP 回显正文 |
| 示例回答 | `已通过 MCP echo 回显：hello-box-mcp。` |

**无 MCP 服务时的最低验收**（端点不可达）：

| 仍应通过 | 仍算失败 |
|----------|----------|
| 加号能选 MCP 插件；发消息后模型**尝试**调用 `echo`；toolEnd 带明确错误（连接失败等） | 加号列表无 MCP；或提示「对话加号仅支持技能、工具与 MCP」以外的类别 |

MCP 端点配置见 [README.md](./README.md)「MCP 可选环境」。

---

## TC-04 回归：禁止类插件

| 项 | 内容 |
|----|------|
| 操作 | 用 API 故意传 `pluginIds` 含已安装的**知识库**市场插件 ID |
| 预期 | HTTP 400，文案含「对话加号仅支持技能、工具与 MCP」 |

---

## TC-05 组合（可选）

| 项 | 内容 |
|----|------|
| 操作 | 加号同时选 **Skill + Tool**（2 个） |
| 用户消息 | `先按技能格式回答「什么是插件市场」，再调用 HTTP 工具 GET，参数 name=combo-test` |
| 预期 | 回答前半满足 Skill 结构；后半出现 tool 事件且 args.name=combo-test |

---

## 记录模板（测试时复制）

```text
日期：
环境：box-server / box-web 分支：
Agent ID：
插件 ID（catalog）：
用户消息：
tool 事件（有/无，toolKey）：
助手首段（前 200 字）：
结论：Pass / Fail
备注：
```
