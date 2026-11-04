# 智能体对话 Agent Chat

矢量图：[`assets/flow-agent-chat.svg`](./assets/flow-agent-chat.svg)

生产环境只读 **已发布版本 Published Version**。  
接口 Endpoint：`POST /api/v1/agents/{id}/chat`  
实现 Implementation：`box-agent` · `AgentChatExecutor`

## 流程图 Flow

```mermaid
flowchart LR
  A["1. 用户请求 Request"] --> B["2. 鉴权 Auth<br/>JWT / API Key"]
  B --> C["3. 加载已发布配置 Load published config"]
  C --> D["4. 构建执行上下文 Build ExecutionContext"]
  D --> E["5. RAG 混合检索 Hybrid search<br/>可选 optional"]
  E --> F["6. 工具执行 Tool calling<br/>可选 optional"]
  F --> G["7. LangChain4j → 大模型 LLM"]
  G --> H["8. SSE 推送 delta / done"]
  H --> I["并行落库 Persist<br/>消息 Message · 追踪 Trace · Token 用量"]
```

## 时序图 Sequence

```mermaid
sequenceDiagram
  autonumber
  actor User as 用户 User
  participant Web as 前端 box-web
  participant API as 接口层 API /api/v1
  participant Auth as 鉴权 Auth JWT / API Key
  participant Agent as 智能体运行时 AgentChatExecutor
  participant RAG as 知识检索 Knowledge RAG
  participant Tool as 工具 Tool / MCP
  participant LLM as 大模型 LLM via LangChain4j
  participant DB as 落库 MySQL / Trace

  User->>Web: 发送消息 Send message
  Web->>API: POST /agents/{id}/chat stream=true
  API->>Auth: 校验身份 Verify
  Auth-->>API: 通过 OK
  API->>Agent: 加载已发布配置 Load Published Version
  Agent->>Agent: 构建执行上下文 Build ExecutionContext
  opt 绑定了知识库 Knowledge bound
    Agent->>RAG: 混合检索 Hybrid search
    RAG-->>Agent: 引用 citations
    Agent-->>Web: SSE type=citations
  end
  Agent->>LLM: 流式生成 Stream generate
  loop 模型输出 Model tokens
    LLM-->>Agent: token 增量
    Agent-->>Web: SSE type=delta
  end
  opt 需要调工具 Tool calling
    Agent-->>Web: SSE type=tool.start
    Agent->>Tool: 执行 Execute
    Tool-->>Agent: 结果 Result
    Agent-->>Web: SSE type=tool.end
    Agent->>LLM: 带工具结果继续 Continue with tool result
  end
  Agent-->>Web: SSE type=done executionId
  par 并行落库 Persist
    Agent->>DB: 消息 Message
    Agent->>DB: 追踪 Trace Span
    Agent->>DB: Token 用量 Usage
  end
  Web-->>User: 渲染完整回答 Render answer
```

## SSE 事件 Events

| type | 中文 | English |
|------|------|---------|
| `citations` | RAG 引用列表 | RAG citation list |
| `delta` | 回答文本增量 | Answer text chunk |
| `tool.start` / `tool.delta` / `tool.end` | 工具调用生命周期 | Tool call lifecycle |
| `tool.confirm` | 危险工具需用户确认 | Dangerous tool confirmation |
| `done` | 流结束，含 executionId | Stream complete |
| `error` | 错误信息 | Error |
