# 对话加号插件测试夹具

用于手工验证：**插件市场安装** → **对话页加号** → 发消息时 **Skill / HTTP 工具 / MCP** 是否按设计生效。

与知识库、工作流无关（后者在 **智能体编排** 里绑定，不应出现在加号列表）。

---

## 目录

| 文件 | 说明 |
|------|------|
| [CASES.md](./CASES.md) | 用例表：用户消息、预期现象、**示例回答** |
| [dev-insert-test-plugins.sql](./dev-insert-test-plugins.sql) | 向 `plugin_catalog` 写入 3 个 `[测试]` 上架插件 |
| [manifests/](./manifests/) | 后台新建插件时可粘贴的 manifest JSON |

---

## 1. 准备数据

```bash
# MySQL（按你本地连接方式调整）
mysql -h 127.0.0.1 -u root -p box < test-fixtures/conversation-plugins/dev-insert-test-plugins.sql
```

或在 **管理后台 → 插件目录** 新建 3 条记录，`category` 分别为 `tools` / `skills` / `mcp`，`manifest_json` 复制 `manifests/` 下对应文件，状态 **已审核 + 已上架**。

> 说明：迁移 `V48` 已把早期种子插件下架，请用本目录的 `fixture-chat-*` 编码，不要依赖 `tool-1` / `mcp-1` 等旧种子。

---

## 2. 工作空间侧

1. 登录 Box Web，进入目标工作空间。
2. **插件市场** → 安装：
   - **全部插件**页：可见 `[测试] HTTPBin GET`、`[测试] MCP Echo`（以及工作空间自建的测试类插件）
   - **发现 → 技能**（侧栏单独入口，数字为技能数量）：在这里找并安装 **`[测试] 三条要点 Skill`**  
     > 产品设计：Skill **不会**出现在「全部插件」列表里，只在「技能」页展示。
   - MCP 完整联调时再装 `[测试] MCP Echo`（也可在全部插件里装）
3. 创建或打开一个 **已配置可用模型** 的智能体（建议**不要**在编排里绑 HTTP/MCP，避免与加号插件混淆）。
4. 进入 **对话** → 输入框 **加号 → 插件**，应只看到 **skills / tools / mcp** 类已安装项（无知识库、工作流）。

---

## 3. 执行用例

打开 [CASES.md](./CASES.md)，按 **TC-01 ~ TC-03** 依次测。每条用例包含：

- 勾选哪个插件
- **用户消息**（可直接复制）
- **预期现象**（Skill 格式 / tool 事件 / MCP 回显）
- **示例回答**（便于对照，模型原文可略有差异）

---

## 4. MCP 可选环境（TC-03 完整成功）

插件 manifest 默认端点：`http://127.0.0.1:3100/mcp`，工具名 `echo`。你需要在本机起一个 **符合 Box MCP HTTP 协议** 的服务，或在安装后：

**插件市场 → 我的 MCP** → 编辑该 Server → 改成你可用的端点 → **同步工具目录**（若 UI 提供）。

若暂时没有 MCP 服务：

- 仍可按 [CASES.md](./CASES.md) 中「无 MCP 服务时的最低验收」记录结果；
- Skill / Tool 两条不依赖 MCP，应优先测通。

---

## 5. 快速复制：三条用户消息

```text
# TC-01 Skill
用一句话说明 Box AI 是什么

# TC-02 Tool
请调用当前可用的 HTTP 工具，用 GET 请求，查询参数 name=box-plugin-test，把工具返回的 JSON 原文贴出来。

# TC-03 MCP
请调用 MCP 工具 echo，参数 message=hello-box-mcp，并告诉我工具返回内容。
```

---

## 6. 与实现的对应关系（排查用）

| 能力 | 后端入口 |
|------|----------|
| 加号 pluginIds 校验 | `MessagePluginContextService`（仅 skills/tools/mcp） |
| 解析 resourceId / Skill 文案 | `ConversationPluginApplicationService` |
| 并入当轮工具 & Skill prompt | `AgentChatPreparer` + `ConversationApplicationService.buildPreparedChat` |
| Tool 执行 | `AgentToolRuntimeService` + `PreparedAgentChat.resolvedTools` |

---

## 7. 重新生成 SQL（可选）

若改了 `manifests/*.json`，可手改 `dev-insert-test-plugins.sql` 中 `manifest_json` 字段，或自行在后台更新插件 manifest。
