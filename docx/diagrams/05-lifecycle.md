# 配置与发布生命周期 Lifecycle

矢量图：[`assets/flow-lifecycle.svg`](./assets/flow-lifecycle.svg)

草稿 Draft 与已发布 Published 严格隔离。生产只跑已发布版本。

```mermaid
flowchart LR
  D["草稿编辑 Draft<br/>Builder"] --> C["配置校验 Validate<br/>模型 Model / 工具 Tool / 知识库 Knowledge"]
  C --> V["生成版本 Create AgentVersion"]
  V --> P["发布上线 Publish"]
  P --> W["对话工作台 Web Chat"]
  P --> A["开放接口 Open API<br/>API Key"]
  P --> E["嵌入站点 Embed"]
```

运行时分工 Runtime split：

- 对话 Chat：`box-agent` · `AgentChatExecutor`
- 工作流 Workflow：`box-runtime` · `WorkflowExecutor`
