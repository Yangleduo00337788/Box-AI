Box 第六部分：LangChain4j + Model Provider + Agent Runtime

一、这一部分要解决什么问题

Box 不是简单的：

用户问题
 ↓
调用 OpenAI
 ↓
返回答案

而是：

用户请求
 ↓
找到 Agent
 ↓
找到已发布 Version
 ↓
加载 Agent 配置
 ↓
加载 Model
 ↓
构建 Prompt
 ↓
加载 Conversation Memory
 ↓
执行 Knowledge RAG
 ↓
加载 Tools
 ↓
构建 LangChain4j ChatModel
 ↓
执行 Agent Runtime
 ↓
流式生成
 ↓
SSE 返回前端
 ↓
记录 Conversation
 ↓
记录 Trace
 ↓
统计 Token / Latency / Cost

因此 Agent Runtime 必须成为整个系统的核心。

---

二、整体架构

                         ┌────────────────────┐
                         │      Vue 3          │
                         │  TDesign Vue Next    │
                         └─────────┬──────────┘
                                   │
                                   │ HTTP / SSE
                                   ▼
                         ┌────────────────────┐
                         │    Spring Boot     │
                         │      API Layer     │
                         └─────────┬──────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │       Agent Runtime         │
                    │                             │
                    │  AgentExecutionService      │
                    │  AgentContextBuilder        │
                    │  PromptService              │
                    │  MemoryService              │
                    │  KnowledgeService           │
                    │  ToolService                │
                    │  ModelService               │
                    │  TraceService               │
                    └──────────────┬──────────────┘
                                   │
             ┌─────────────────────┼────────────────────┐
             │                     │                    │
             ▼                     ▼                    ▼
       ┌───────────┐        ┌────────────┐       ┌────────────┐
       │ LangChain4j│        │   Redis    │       │   MySQL    │
       │            │        │            │       │            │
       │ ChatModel  │        │ Memory     │       │ Agent      │
       │ Embedding  │        │ Cache      │       │ Model      │
       │ Tools      │        │ RateLimit  │       │ Config     │
       └─────┬─────┘        └────────────┘       └────────────┘
             │
       ┌─────┴───────────┐
       │                 │
       ▼                 ▼
 ┌────────────┐   ┌─────────────┐
 │ LLM Provider│   │ Elasticsearch│
 │             │   │             │
 │ OpenAI      │   │ Hybrid RAG  │
 │ DeepSeek    │   │ Vector      │
 │ Qwen        │   │ Keyword     │
 │ GLM         │   │             │
 │ Ollama      │   │             │
 └─────────────┘   └─────────────┘

---

三、核心设计原则

Agent Runtime 必须做到：

1. 不直接依赖某一家模型

错误设计：

OpenAiChatModel model = OpenAiChatModel.builder()
        .apiKey(...)
        .modelName(...)
        .build();

然后整个系统都使用 OpenAI。

这种设计后期非常难扩展。

应该设计成：

Agent Runtime
      ↓
ModelFactory
      ↓
ChatModel
      ↓
LangChain4j
      ↓
具体 Provider

例如：

OpenAI
DeepSeek
Qwen
GLM
Ollama
Azure OpenAI
其他 OpenAI Compatible API

都统一成：

ChatModel

---

四、Model Provider 设计

数据库中建议至少存在：

model_provider
model_definition
model_credential

关系：

Provider
   │
   ├── Model A
   ├── Model B
   └── Model C

例如：

DeepSeek
 ├── deepseek-chat
 └── deepseek-reasoner

OpenAI
 ├── gpt-4.1
 └── gpt-4.1-mini

Qwen
 ├── qwen-plus
 └── qwen-max

Ollama
 ├── qwen3
 └── llama3

---

五、ModelProvider

领域层：

public class ModelProvider {

    private Long id;

    private String providerCode;

    private String providerName;

    private String providerType;

    private String baseUrl;

    private Integer status;

    private Long workspaceId;
}

例如：

providerCode = openai
providerName = OpenAI
providerType = OPENAI_COMPATIBLE
baseUrl = https://api.openai.com/v1

DeepSeek：

providerCode = deepseek
providerType = OPENAI_COMPATIBLE
baseUrl = https://api.deepseek.com

Qwen：

providerCode = qwen
providerType = OPENAI_COMPATIBLE

这样大量模型都可以通过统一适配。

---

六、ModelDefinition

public class ModelDefinition {

    private Long id;

    private Long providerId;

    private String modelCode;

    private String modelName;

    private String modelType;

    private Boolean supportStreaming;

    private Boolean supportToolCalling;

    private Boolean supportVision;

    private Integer contextWindow;

    private Integer maxOutputTokens;

    private String metadata;

    private Integer status;
}

例如：

modelCode:
deepseek-chat

modelType:
CHAT

supportStreaming:
true

supportToolCalling:
true

---

七、ModelCredential

模型 API Key 绝对不能明文直接存数据库。

数据库：

model_credential

保存：

id
provider_id
workspace_id
credential_name
encrypted_api_key
encrypted_secret
status
created_at
updated_at

真正的：

sk-xxxxxxxx

必须加密保存。

---

八、CredentialService

设计：

public interface CredentialService {

    String getApiKey(Long providerId, Long workspaceId);

    void saveCredential(ModelCredentialCommand command);

    void deleteCredential(Long credentialId);
}

底层：

MySQL
 ↓
读取密文
 ↓
AES/GCM 解密
 ↓
Runtime 使用

不要：

log.info("apiKey={}", apiKey);

任何日志都不能打印：

API Key
JWT
Authorization
密码
Secret

---

九、ModelFactory

这是整个模型系统最重要的组件之一。

public interface ModelFactory {

    ChatModel createChatModel(ModelRuntimeConfig config);

    StreamingChatModel createStreamingChatModel(
            ModelRuntimeConfig config
    );
}

---

十、ModelRuntimeConfig

public class ModelRuntimeConfig {

    private String providerCode;

    private String modelCode;

    private String baseUrl;

    private String apiKey;

    private Double temperature;

    private Double topP;

    private Integer maxTokens;

    private Boolean streaming;

    private Map<String, Object> options;
}

例如：

{
  "providerCode": "deepseek",
  "modelCode": "deepseek-chat",
  "temperature": 0.7,
  "topP": 0.9,
  "maxTokens": 4096,
  "streaming": true
}

---

十一、OpenAI Compatible Provider

由于大量国内模型兼容 OpenAI API，因此第一阶段建议重点支持：

OpenAI Compatible

例如：

OpenAI
DeepSeek
Qwen
Moonshot
GLM
Ollama
SiliconFlow
其他兼容 OpenAI API 的服务

架构：

OpenAICompatibleModelFactory
              │
              ▼
      OpenAI-compatible API

这样不会为了每一家模型写一套 Runtime。

---

十二、ModelFactory 实现

例如：

@Component
public class DefaultModelFactory implements ModelFactory {

    @Override
    public ChatModel createChatModel(
            ModelRuntimeConfig config
    ) {

        if ("openai".equalsIgnoreCase(config.getProviderCode())) {
            return createOpenAI(config);
        }

        if ("deepseek".equalsIgnoreCase(config.getProviderCode())) {
            return createOpenAICompatible(config);
        }

        if ("qwen".equalsIgnoreCase(config.getProviderCode())) {
            return createOpenAICompatible(config);
        }

        if ("ollama".equalsIgnoreCase(config.getProviderCode())) {
            return createOllama(config);
        }

        throw new BusinessException(
                "Unsupported model provider: "
                        + config.getProviderCode()
        );
    }
}

但后期建议进一步演进为：

ModelProviderAdapter
        │
        ├── OpenAIAdapter
        ├── OpenAICompatibleAdapter
        ├── OllamaAdapter
        └── ...

避免 Factory 越来越庞大。

---

十三、ModelProviderAdapter

public interface ModelProviderAdapter {

    String providerCode();

    ChatModel createChatModel(
            ModelRuntimeConfig config
    );

    StreamingChatModel createStreamingChatModel(
            ModelRuntimeConfig config
    );
}

实现：

OpenAIProviderAdapter
DeepSeekProviderAdapter
QwenProviderAdapter
OllamaProviderAdapter

---

十四、最终模型创建流程

Agent
 ↓
Agent Version
 ↓
modelId
 ↓
ModelDefinition
 ↓
ModelProvider
 ↓
Credential
 ↓
ModelRuntimeConfig
 ↓
ModelProviderAdapter
 ↓
LangChain4j ChatModel

这样 Agent 本身完全不关心：

OpenAI
DeepSeek
Qwen
Ollama

---

十五、Agent Runtime

Runtime 是 Box 的核心。

建议：

box-runtime

目录：

box-runtime
└── src/main/java/com/boxai/runtime

    ├── controller
    │
    ├── application
    │   ├── AgentRuntimeService.java
    │   ├── WorkflowRuntimeService.java
    │   └── RuntimeFacade.java
    │
    ├── context
    │   ├── AgentExecutionContext.java
    │   ├── UserContext.java
    │   ├── ConversationContext.java
    │   └── RuntimeVariableContext.java
    │
    ├── executor
    │   ├── AgentExecutor.java
    │   ├── LlmExecutor.java
    │   ├── ToolExecutor.java
    │   └── KnowledgeExecutor.java
    │
    ├── model
    │   ├── ModelFactory.java
    │   └── ModelRuntimeConfig.java
    │
    ├── prompt
    │   ├── PromptBuilder.java
    │   └── PromptTemplateEngine.java
    │
    ├── memory
    │   ├── MemoryService.java
    │   └── ChatMemoryProvider.java
    │
    ├── knowledge
    │   ├── KnowledgeRetriever.java
    │   └── RetrievalService.java
    │
    ├── tool
    │   ├── ToolRegistry.java
    │   └── ToolExecutor.java
    │
    ├── stream
    │   ├── StreamEvent.java
    │   └── StreamPublisher.java
    │
    └── trace
        ├── TraceContext.java
        └── TraceRecorder.java

---

十六、AgentExecutionContext

Agent 执行过程中需要一个统一上下文。

public class AgentExecutionContext {

    private String executionId;

    private String traceId;

    private Long userId;

    private Long workspaceId;

    private Long agentId;

    private Long agentVersionId;

    private String conversationId;

    private String message;

    private Map<String, Object> variables;

    private Map<String, Object> metadata;
}

它非常重要。

整个 Runtime：

Prompt
Memory
RAG
Tool
LLM
Trace

都从这里读取上下文。

---

十七、ExecutionId 与 TraceId

每次执行：

executionId

代表一次完整 Agent 执行。

例如：

executionId:
exec_01JXYZ...

而：

traceId

用于追踪整个链路。

例如：

traceId
 │
 ├── Prompt
 │
 ├── Memory
 │
 ├── Knowledge
 │
 ├── Tool
 │
 └── LLM

---

十八、AgentRuntimeService

核心接口：

public interface AgentRuntimeService {

    AgentExecutionResult execute(
            AgentExecutionRequest request
    );

    void stream(
            AgentExecutionRequest request,
            StreamObserver observer
    );
}

---

十九、AgentExecutionRequest

public class AgentExecutionRequest {

    private Long agentId;

    private Long agentVersionId;

    private Long userId;

    private Long workspaceId;

    private String conversationId;

    private String message;

    private Map<String, Object> variables;
}

---

二十、Agent Runtime 完整流程

POST /api/v1/runtime/agents/{agentId}/chat
                 │
                 ▼
        Authentication
                 │
                 ▼
        Workspace Check
                 │
                 ▼
          Load Agent
                 │
                 ▼
       Load Published Version
                 │
                 ▼
     Build Execution Context
                 │
                 ▼
        Load Model Config
                 │
                 ▼
       Build Prompt Context
                 │
                 ▼
         Load Memory
                 │
                 ▼
         Execute RAG
                 │
                 ▼
         Register Tools
                 │
                 ▼
        Create ChatModel
                 │
                 ▼
       LangChain4j Execute
                 │
                 ▼
         Streaming Token
                 │
                 ▼
            SSE
                 │
                 ▼
      Save Conversation
                 │
                 ▼
        Record Trace
                 │
                 ▼
       Update Analytics

---

二十一、Prompt Builder

Agent 的 Prompt 不应该直接拼接字符串。

应该：

public interface PromptBuilder {

    PromptBuildResult build(
            AgentVersion version,
            AgentExecutionContext context
    );
}

最终生成：

System Prompt
+
Variables
+
Conversation History
+
Knowledge Context
+
Tool Instructions
+
User Message

---

二十二、Prompt 模板

例如 Agent 配置：

你是一名专业 Java 架构师。

用户姓名：
{{user_name}}

当前日期：
{{current_date}}

请结合知识库内容回答用户问题。

Runtime：

{{user_name}}
        ↓
张三

{{current_date}}
        ↓
2026-09-10

最终：

你是一名专业 Java 架构师。

用户姓名：
张三

当前日期：
2026-09-10

---

二十三、变量系统

变量来源：

SYSTEM
USER
CONVERSATION
RUNTIME
WORKFLOW
CUSTOM

例如：

{{user.name}}
{{conversation.id}}
{{agent.name}}
{{runtime.execution_id}}

建议建立：

RuntimeVariableResolver

负责：

Object resolve(
        String expression,
        AgentExecutionContext context
);

---

二十四、Chat Memory

第一阶段不要把 Memory 做得过于复杂。

建议：

短期记忆
+
可选长期记忆

短期记忆：

Conversation
 ↓
Messages
 ↓
Redis / MySQL
 ↓
LangChain4j ChatMemory

---

二十五、ChatMemoryProvider

public interface ChatMemoryProvider {

    ChatMemory getMemory(
            String conversationId
    );

    void clear(
            String conversationId
    );
}

建议：

conversation_id
        ↓
Redis
        ↓
最近 N 条消息

而完整聊天记录：

MySQL

保存。

这样：

MySQL = 持久化
Redis = 热数据
LangChain4j = Runtime Memory

---

二十六、为什么不能无限塞历史消息

例如用户和 Agent 聊了：

1000 条消息

如果全部发送给 LLM：

Token 爆炸

因此需要：

最近消息
+
历史摘要

结构：

Conversation
 ├── Summary
 │
 └── Recent Messages

例如：

历史摘要：
用户正在开发 Box 平台，后端使用 Java 21，
前端使用 Vue 3，数据库使用 MySQL。

最近消息：
User: ...
Assistant: ...
User: ...

---

二十七、Knowledge Retriever

Agent Runtime 不直接操作 Elasticsearch。

设计：

public interface KnowledgeRetriever {

    List<RetrievedDocument> retrieve(
            String query,
            KnowledgeRetrievalConfig config
    );
}

Runtime：

User Query
 ↓
KnowledgeRetriever
 ↓
Elasticsearch
 ↓
Vector Search
 ↓
Keyword Search
 ↓
Hybrid Search
 ↓
Rerank
 ↓
Top K

---

二十八、RAG Context Builder

搜索结果不能直接原样塞给模型。

应该：

public interface RetrievalContextBuilder {

    String build(
            List<RetrievedDocument> documents
    );
}

例如：

[文档1]
标题：Java并发编程
内容：......

[文档2]
标题：Spring Boot
内容：......

最终：

以下内容来自知识库，仅作为参考：

<context>
...
</context>

然后进入 Prompt。

---

二十九、Tool Registry

Agent 运行时需要动态加载工具。

public interface ToolRegistry {

    List<Tool> loadTools(
            Long agentId,
            Long agentVersionId
    );
}

工具：

HTTP Tool
Function Tool
Database Tool
Code Tool
MCP Tool

LangChain4j：

Agent Runtime
      ↓
ToolRegistry
      ↓
List<Tool>
      ↓
LangChain4j

---

三十、工具调用流程

例如用户：

查询今天的天气

LLM 判断：

需要调用 weatherTool

流程：

User
 ↓
LLM
 ↓
Tool Call
 ↓
ToolRegistry
 ↓
Weather Tool
 ↓
Tool Result
 ↓
LLM
 ↓
Final Answer

Trace：

LLM
 └── Tool Call
      └── HTTP
           └── Result

---

三十一、Tool 必须有权限

绝对不能：

Agent绑定了Tool
 ↓
任何用户直接执行

必须：

User
 ↓
Workspace
 ↓
Agent
 ↓
Agent Version
 ↓
Tool Binding
 ↓
Permission
 ↓
Tool Execute

对于危险工具：

删除数据
执行 SQL
调用内部接口
执行代码

还需要：

Confirmation

---

三十二、Streaming

Agent 平台必须支持流式输出。

前端：

POST /api/v1/runtime/agents/{agentId}/chat

或者：

GET /api/v1/runtime/agents/{agentId}/stream

实际项目建议使用：

POST + SSE

因为用户输入可能比较复杂。

---

三十三、SSE 数据格式（当前实现）

**实现类**：`com.boxai.agent.api.ChatStreamEvent`  
**发送方**：`com.boxai.agent.chat.AgentChatExecutor`  
**入口**：`POST /api/v1/agents/{id}/chat`（`stream=true`）、`POST /api/v1/conversations/{id}/messages`、`POST /api/v1/published/agents/{id}/chat`

每条 SSE 帧为 `data: {json}\n\n`，`json` 字段：

| type | content | message | executionId | 说明 |
|------|---------|---------|---------------|------|
| `citations` | RAG 引用 JSON 数组 | — | — | 流开始前推送 |
| `delta` | 文本片段 | — | — | 模型回答增量 |
| `tool.start` | Tool payload JSON | — | — | 工具开始执行 |
| `tool.delta` | Tool payload JSON | — | — | 工具输出增量 |
| `tool.end` | Tool payload JSON | — | — | 工具结束（含 status） |
| `done` | — | — | 可选 | 流结束 |
| `error` | — | 错误信息 | — | 失败 |

Tool payload 示例：

```json
{
  "toolKey": "get_weather",
  "arguments": { "city": "上海" },
  "output": "{...}",
  "status": "SUCCEEDED"
}
```

`delta` 示例：

```json
{"type":"delta","content":"你好"}
```

`done` 示例：

```json
{"type":"done","executionId":12345}
```

---

三十四、统一 StreamEvent（设计目标 vs 当前代码）

**设计目标**（远期统一事件模型）仍保留 `StreamEvent` + `StreamEventType` 枚举（`MESSAGE_START`、`TOKEN`、`TOOL_CALL` 等）。

**V1 已落地**：上表 `ChatStreamEvent` record，类型字符串为 `delta` / `citations` / `tool.start` / `tool.delta` / `tool.end` / `done` / `error`。前端解析见 `box-web/src/api/chatStream.ts`。

**Agent Runtime 主类（代码）**：

| 文档名 | 实际类 / 模块 |
|--------|----------------|
| AgentExecutionService | `AgentChatExecutor` + `AgentChatPreparer`（`box-modules/box-agent`） |
| ToolService | `AgentToolRuntimeService` |
| TraceService | `ExecutionRecorder`（`box-trace`） |
| KnowledgeService | `KnowledgeSearchService`（`box-knowledge`） |

---

三十五、为什么需要 THINKING

这里不要直接暴露模型内部 Chain-of-Thought。

可以显示：

正在检索知识库...
正在调用工具...
正在生成回答...

而不是：

模型内部完整思考过程

---

三十六、Trace

每一个 Runtime 都应该生成 Trace。

例如：

Trace
 │
 ├── Agent
 │
 ├── Prompt
 │
 ├── Memory
 │
 ├── Knowledge
 │    ├── Search
 │    └── Rerank
 │
 ├── Tool
 │
 └── LLM

每一个 Span：

public class TraceSpan {

    private String spanId;

    private String traceId;

    private String parentSpanId;

    private String type;

    private String name;

    private Long startTime;

    private Long endTime;

    private Long duration;

    private String status;

    private String input;

    private String output;

    private Integer inputTokens;

    private Integer outputTokens;
}

---

三十七、Trace 示例

traceId = trace_001

：

Agent Execution
duration: 5.8s

 ├── Prompt
 │   duration: 10ms
 │
 ├── Memory
 │   duration: 5ms
 │
 ├── Knowledge Search
 │   duration: 320ms
 │
 ├── Tool Call
 │   duration: 800ms
 │
 └── LLM
     duration: 4.6s
     inputTokens: 1850
     outputTokens: 632

前端可以做成类似：

Execution
├── Agent
├── Prompt
├── Knowledge
├── Tool
└── LLM

点击节点：

Input
Output
Duration
Tokens
Status

---

三十八、Token Usage

模型调用后需要统一统计：

public class TokenUsage {

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;
}

最终：

Execution
 ↓
TokenUsage
 ↓
Analytics

未来计费系统也依赖这个。

---

三十九、Cost

建议 Runtime 就开始预留：

public class ModelUsage {

    private String provider;

    private String model;

    private Integer inputTokens;

    private Integer outputTokens;

    private BigDecimal inputCost;

    private BigDecimal outputCost;

    private BigDecimal totalCost;
}

即使 MVP 阶段暂时不收费，也可以先记录。

---

四十、AgentExecutor

AgentExecutor 是核心执行器。

public interface AgentExecutor {

    AgentExecutionResult execute(
            AgentExecutionContext context
    );

    void stream(
            AgentExecutionContext context,
            StreamObserver observer
    );
}

实现：

DefaultAgentExecutor

---

四十一、DefaultAgentExecutor

逻辑：

public void stream(
        AgentExecutionContext context,
        StreamObserver observer
) {

    // 1. Load Agent Version

    // 2. Build Prompt

    // 3. Load Memory

    // 4. Build Knowledge Retriever

    // 5. Load Tools

    // 6. Create Model

    // 7. Execute LangChain4j

    // 8. Stream tokens

    // 9. Save conversation

    // 10. Record trace
}

但生产代码不要真的把所有逻辑塞进一个方法。

应该进一步拆分。

---

四十二、Runtime Pipeline

建议最终设计成 Pipeline：

AgentRuntimePipeline

    ↓

ContextStage

    ↓

AgentVersionStage

    ↓

PromptStage

    ↓

MemoryStage

    ↓

KnowledgeStage

    ↓

ToolStage

    ↓

ModelStage

    ↓

LLMStage

    ↓

ResponseStage

    ↓

PersistenceStage

    ↓

TraceStage

---

四十三、RuntimeStage

public interface RuntimeStage {

    void execute(
            AgentExecutionContext context
    );
}

不过流式执行和普通执行最好不要简单共用同一接口。

可以设计：

public interface RuntimeStage {

    RuntimeStageResult execute(
            AgentExecutionContext context
    );
}

未来：

Sync Runtime
Streaming Runtime
Workflow Runtime

可以共享底层组件。

---

四十四、Agent Runtime 与 Workflow Runtime

两者不要混成一个东西。

Agent Runtime

负责：

Prompt
Memory
RAG
Tool
LLM

而：

Workflow Runtime

负责：

Node
 ↓
Condition
 ↓
LLM
 ↓
Tool
 ↓
Loop
 ↓
Parallel

关系：

Workflow Runtime
       │
       ├── LLM Node
       │       ↓
       │   Agent Runtime Component
       │
       ├── Knowledge Node
       │
       └── Tool Node

---

四十五、LangChain4j 在系统中的位置

不要让 LangChain4j 渗透整个业务系统。

错误：

Controller
 ↓
LangChain4j
 ↓
LLM

正确：

Controller
 ↓
Application
 ↓
Agent Runtime
 ↓
Runtime Adapter
 ↓
LangChain4j
 ↓
LLM

这样以后即使：

LangChain4j

更换为其他框架，业务层也不用大面积修改。

---

四十六、推荐的依赖方向

box-bootstrap
       ↓
box-application
       ↓
box-runtime
       ↓
box-domain

LangChain4j 尽量集中在：

box-runtime
box-infrastructure

不要让：

box-user
box-workspace

直接依赖 LangChain4j。

---

四十七、Runtime API

开发调试：

POST /api/v1/runtime/agents/{agentId}/execute

流式：

POST /api/v1/runtime/agents/{agentId}/stream

指定版本：

POST /api/v1/runtime/agents/{agentId}/versions/{versionId}/stream

但是生产发布 API 不建议让用户随意指定 Version。

应该：

Agent
 ↓
Published Version

自动加载。

---

四十八、请求示例

{
  "conversationId": "conv_001",
  "message": "请介绍一下 Box 的整体架构",
  "variables": {
    "language": "zh-CN"
  }
}

---

四十九、Runtime 返回

非流式：

{
  "executionId": "exec_001",
  "messageId": "msg_001",
  "content": "Box 是一个企业级 AI Agent 平台……",
  "usage": {
    "inputTokens": 1200,
    "outputTokens": 500,
    "totalTokens": 1700
  }
}

---

五十、流式响应

message_start

token
"Box"

token
" 是"

token
"一个"

token
"企业级"

token
" AI Agent"

citation

message_end

---

五十一、异常体系

模型异常不能直接返回：

NullPointerException

统一：

public enum RuntimeErrorCode {

    MODEL_NOT_FOUND,

    MODEL_UNAVAILABLE,

    MODEL_TIMEOUT,

    MODEL_RATE_LIMIT,

    TOOL_EXECUTION_FAILED,

    KNOWLEDGE_SEARCH_FAILED,

    MEMORY_LOAD_FAILED,

    RUNTIME_TIMEOUT,

    RUNTIME_CANCELLED
}

---

五十二、模型超时

必须设置：

connect timeout
read timeout
overall timeout

例如：

连接：
10s

读取：
60s

Agent：
120s

实际数值应该配置化。

---

五十三、重试策略

不要所有错误都重试。

可以：

网络错误        → 可重试
429             → 根据 Retry-After
5xx             → 可有限重试
参数错误        → 不重试
权限错误        → 不重试
Token 超限      → 不重试
Tool 参数错误   → 不自动无限重试

例如：

最多 2~3 次
指数退避

---

五十四、Rate Limit

第一阶段：

Redis

例如：

用户
 ↓
API
 ↓
Redis RateLimiter
 ↓
Agent Runtime

限制：

IP
User
Workspace
API Key
Agent

---

五十五、Runtime Cancellation

用户点击：

停止生成

后端需要：

Cancel execution

因此 ExecutionContext 最好支持：

private AtomicBoolean cancelled;

或者使用：

CancellationToken

当：

cancelled = true

Runtime：

停止 LLM Stream
停止 Tool
停止后续执行
保存当前状态

---

五十六、并发控制

一个 Agent 可能同时：

1000 个请求

因此不能：

synchronized(agent)

这种粗粒度锁。

建议：

Redis
+
线程池 / 虚拟线程
+
超时控制
+
限流

Java 21 可以合理使用 Virtual Threads，但必须结合实际 SDK 的阻塞模型进行验证。

---

五十七、Execution 生命周期

CREATED
   ↓
RUNNING
   ↓
SUCCESS

异常：

RUNNING
   ↓
FAILED

取消：

RUNNING
   ↓
CANCELLED

超时：

RUNNING
   ↓
TIMEOUT

---

五十八、数据库 execution 表

建议：

agent_execution

字段：

id
execution_id
workspace_id
agent_id
agent_version_id
conversation_id
user_id
status
input_text
output_text
input_tokens
output_tokens
total_tokens
duration_ms
error_code
error_message
started_at
finished_at
created_at

---

五十九、不要把完整 Trace 塞 execution 表

：

agent_execution

负责：

一次执行的摘要

：

trace_span

负责：

完整执行链路

关系：

Execution
   │
   ├── Span
   ├── Span
   ├── Span
   └── Span

---

六十、Conversation 与 Runtime

一次聊天：

Conversation

下面有：

Message

而一次 Message 可能对应：

Execution

关系：

Conversation
      │
      ├── Message
      │      │
      │      └── Execution
      │              ├── Prompt
      │              ├── RAG
      │              ├── Tool
      │              └── LLM
      │
      └── Message

---

六十一、最终完整执行架构

                    USER
                     │
                     ▼
              Agent Chat API
                     │
                     ▼
               Authentication
                     │
                     ▼
              Workspace Check
                     │
                     ▼
                Agent Runtime
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
   Published Version       Execution Context
          │                     │
          └──────────┬──────────┘
                     ▼
                Prompt Builder
                     │
                     ▼
                 Chat Memory
                     │
                     ▼
               Knowledge RAG
                     │
                     ▼
                Tool Registry
                     │
                     ▼
                 ModelFactory
                     │
                     ▼
                LangChain4j
                     │
                     ▼
                     LLM
                     │
             ┌───────┴───────┐
             │               │
             ▼               ▼
         Token Stream      Tool Call
             │               │
             │          Tool Execution
             │               │
             └───────┬───────┘
                     ▼
                  SSE
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
     Conversation   Trace    Analytics

---

六十二、Box 第六部分最终代码结构

box-runtime
│
├── controller
│   └── AgentRuntimeController
│
├── application
│   ├── AgentRuntimeService
│   ├── DefaultAgentRuntimeService
│   └── RuntimeFacade
│
├── context
│   ├── AgentExecutionContext
│   ├── AgentExecutionRequest
│   ├── AgentExecutionResult
│   └── RuntimeVariableContext
│
├── executor
│   ├── AgentExecutor
│   ├── DefaultAgentExecutor
│   ├── LlmExecutor
│   ├── KnowledgeExecutor
│   └── ToolExecutor
│
├── model
│   ├── ModelFactory
│   ├── DefaultModelFactory
│   ├── ModelRuntimeConfig
│   ├── ModelProviderAdapter
│   ├── OpenAIProviderAdapter
│   ├── OpenAICompatibleAdapter
│   └── OllamaProviderAdapter
│
├── prompt
│   ├── PromptBuilder
│   ├── DefaultPromptBuilder
│   ├── PromptTemplateEngine
│   └── RuntimeVariableResolver
│
├── memory
│   ├── MemoryService
│   ├── ChatMemoryProvider
│   └── RedisChatMemoryProvider
│
├── knowledge
│   ├── KnowledgeRetriever
│   ├── RetrievalService
│   └── RetrievalContextBuilder
│
├── tool
│   ├── ToolRegistry
│   ├── ToolExecutor
│   └── ToolPermissionChecker
│
├── stream
│   ├── StreamEvent
│   ├── StreamEventType
│   └── StreamPublisher
│
├── trace
│   ├── TraceContext
│   ├── TraceSpan
│   └── TraceRecorder
│
└── usage
    ├── TokenUsage
    └── ModelUsage

---

六十三、第一阶段不要做的事情

为了避免项目一开始过度设计，暂时不要加入：

Kafka
微服务
Kubernetes
分布式 Agent Runtime
复杂 Multi-Agent
复杂 Planner
复杂 Memory Graph
复杂 Agent-to-Agent
Billing
Marketplace

尤其是：

Agent Runtime

第一版一定要先跑通。

---

六十四、第一阶段真正要跑通的 Demo

最终应该能够做到：

浏览器
 ↓
Vue Agent Chat
 ↓
Spring Boot
 ↓
Agent Runtime
 ↓
读取 Agent Published Version
 ↓
读取 Model
 ↓
读取 API Key
 ↓
Prompt
 ↓
LangChain4j
 ↓
DeepSeek / OpenAI Compatible Model
 ↓
SSE
 ↓
浏览器实时显示

也就是说：

«先实现一个真正可以运行的 Agent。»

然后再把：

Memory
RAG
Tool
Workflow
Trace

逐层接进去。

---

六十五、第六部分完成后的核心能力

完成这一部分后，Box 将正式从：

AI 管理后台

进入：

真正的 AI Agent 平台

核心能力：

✓ 多模型
✓ Model Provider
✓ API Key 管理
✓ Prompt
✓ Variables
✓ Conversation Memory
✓ Knowledge RAG 接口
✓ Tool Registry
✓ LangChain4j
✓ Agent Runtime
✓ Streaming
✓ SSE
✓ Execution
✓ Trace
✓ Token Usage
✓ Timeout
✓ Cancellation
✓ Rate Limit
✓ Error Handling

最终核心链路：

User
 ↓
Agent
 ↓
Published Version
 ↓
Prompt
 ↓
Memory
 ↓
RAG
 ↓
Tools
 ↓
ModelFactory
 ↓
LangChain4j
 ↓
LLM
 ↓
SSE
 ↓
Conversation
 ↓
Trace
 ↓
Analytics

这条链路就是 Box 的第一条“生命线”。

完成它之后，再继续做 Workflow Runtime，就可以真正实现类似 Coze 的可视化 Agent/Workflow 执行体系。