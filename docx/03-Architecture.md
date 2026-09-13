Box Architecture

企业级 AI Agent / Workflow 平台系统架构设计

项目名称： Box
项目定位： 类 Coze / Dify 的企业级 AI Agent 创建、编排、运行与发布平台
文档版本： V1.0
架构模式： Modular Monolith（模块化单体）
后端： Java 21 + Spring Boot 3
AI Framework： LangChain4j
前端： Vue 3 + TypeScript + Vite + TDesign Vue Next
Database： MySQL 8
Cache： Redis
Search / Vector： Elasticsearch
Object Storage： MinIO
消息队列： V1 暂不使用 Kafka
容器： Docker Compose

---

一、总体架构

Box 不采用传统的：

«Controller → Service → Mapper»

这种简单 CRUD 架构作为整个系统的核心。

因为 Box 本质上是一个：

«AI Application Platform + Agent Runtime + Workflow Engine + RAG Platform»

因此需要将系统拆分成多个领域模块。

整体架构：

                         ┌─────────────────────────┐
                         │       用户 / Client      │
                         │                         │
                         │ Web / API / Embed Chat  │
                         └────────────┬────────────┘
                                      │
                                      ▼
                         ┌─────────────────────────┐
                         │       Nginx / Gateway    │
                         └────────────┬────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Box Application                             │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌─────────────────────────┐  │
│  │ Identity      │   │ Workspace    │   │ Agent Management         │  │
│  │ Auth / RBAC   │   │ Tenant       │   │ Agent / Version          │  │
│  └──────────────┘   └──────────────┘   └─────────────────────────┘  │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌─────────────────────────┐  │
│  │ Model         │   │ Knowledge    │   │ Tool Management          │  │
│  │ Provider      │   │ RAG          │   │ HTTP / DB / MCP / Code  │  │
│  └──────────────┘   └──────────────┘   └─────────────────────────┘  │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌─────────────────────────┐  │
│  │ Agent Runtime │   │ Workflow     │   │ Conversation             │  │
│  │ AI Execution  │   │ Runtime      │   │ Chat / Message           │  │
│  └──────────────┘   └──────────────┘   └─────────────────────────┘  │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌─────────────────────────┐  │
│  │ Trace         │   │ Publish      │   │ Analytics                │  │
│  │ Observability │   │ API / Web    │   │ Metrics                  │  │
│  └──────────────┘   └──────────────┘   └─────────────────────────┘  │
│                                                                      │
└───────────────┬───────────────┬───────────────┬──────────────────────┘
                │               │               │
                ▼               ▼               ▼
          ┌──────────┐    ┌──────────┐    ┌─────────────┐
          │  MySQL   │    │  Redis   │    │ Elasticsearch│
          └──────────┘    └──────────┘    └─────────────┘
                                                │
                                                ▼
                                         Vector / Search

                ┌───────────────┐
                │    MinIO      │
                │ Files / Docs  │
                └───────────────┘

---

二、为什么 V1 使用模块化单体

Box 第一阶段明确：

不要微服务
不要 Kafka
不要 Kubernetes
不要服务注册中心
不要分布式事务

采用：

Spring Boot
    ↓
Modular Monolith
    ↓
领域模块隔离
    ↓
未来按模块拆微服务

原因非常简单。

如果一开始就拆：

agent-service
workflow-service
knowledge-service
model-service
tool-service
runtime-service
conversation-service
trace-service

会立即引入：

- RPC
- 服务发现
- 配置中心
- 分布式事务
- 消息队列
- 链路追踪
- 服务间认证
- Docker/K8s 运维
- 多服务调试

对于 V1 来说复杂度远远超过业务本身。

因此：

               Box
                  │
        ┌─────────┴─────────┐
        │ Modular Monolith  │
        └─────────┬─────────┘
                  │
     ┌────────────┼────────────┐
     ▼            ▼            ▼
   Agent       Workflow      Knowledge
     │            │            │
     └────────────┼────────────┘
                  ▼
             Agent Runtime

未来业务规模上来后，再拆：

Box Monolith
      │
      ├── Agent Service
      ├── Workflow Service
      ├── Knowledge Service
      ├── Runtime Service
      └── Model Service

这样成本最低。

---

三、技术栈

3.1 Frontend

Vue 3
TypeScript
Vite
Pinia
Vue Router
Axios
TDesign Vue Next（tdesign-vue-next）
tdesign-icons-vue-next
Vue Flow
Monaco Editor
ECharts
SSE

禁止 Semi Design Vue。

组件文档通过 TDesign MCP 查询，framework 固定 vue-next。

其中：

TDesign Vue Next

负责：

Button
Form
Input
Dialog
Table
Drawer
Select
Tabs
Dropdown
Notification
Message
Layout
Menu
Tree
Upload

Vue Flow

负责：

Workflow Editor
Node
Edge
Canvas
Zoom
Pan
Minimap

Monaco Editor

用于：

Prompt 编辑
JSON 编辑
Code Node
JavaScript
Python
HTTP Body
JSON Schema

---

四、Backend 技术栈

Java 21
Spring Boot 3
Spring MVC
Spring Security
JWT
MyBatis-Flex
MySQL 8
Redis
Elasticsearch
MinIO
LangChain4j
Jackson
Lombok
MapStruct
Validation
OpenAPI
SSE

建议：

MyBatis-Flex

作为数据库访问框架。

不建议 Box V1 同时使用：

JPA
MyBatis
MyBatis-Plus
MyBatis-Flex

避免 ORM 技术混乱。

统一：

MyBatis-Flex

---

五、Maven 多模块结构

整个后端采用 Maven Multi Module。

box-server
│
├── pom.xml
│
├── box-bootstrap
│
├── box-common
│
├── box-security
│
├── box-infrastructure
│
├── box-domain
│
├── box-application
│
└── box-modules
    │
    ├── box-user
    ├── box-workspace
    ├── box-tenant
    ├── box-agent
    ├── box-model
    ├── box-knowledge
    ├── box-tool
    ├── box-workflow
    ├── box-conversation
    ├── box-runtime
    ├── box-publish
    ├── box-trace
    └── box-analytics（`AnalyticsController` · `/api/v1/analytics/overview|trends`）

**Runtime 分工（与代码一致）**：

- **Agent 对话 Runtime**：`box-agent`（`AgentChatExecutor`、`AgentChatPreparer`、`ChatStreamEvent`）
- **Workflow Runtime**：`box-runtime`（`WorkflowExecutor`、各 `*NodeExecutor`）

**前端工程**：

- `box-web` — C 端（对话工作台、Builder、知识库等）
- `box-admin-web` — 平台管理端（租户、套餐、模板、插件）
- `box-ui` — 共享组件库（`@box/ui` alias，Layout / 品牌 / 菜单类型）

---

六、模块职责

6.1 box-bootstrap

系统启动模块。

BoxApplication.java

负责：

Spring Boot 启动
配置加载
Bean 装配
全局异常
Web 配置

---

七、Common

box-common

负责公共能力：

Result
PageResult
ErrorCode
BusinessException
Enums
Constants
Utils
JSON
Date
String

例如：

public record Result<T>(
        Integer code,
        String message,
        T data
) {}

---

八、Security

box-security

负责：

JWT
Authentication
Authorization
RBAC
Workspace Permission
API Key
Security Context

核心：

User
    ↓
Workspace
    ↓
Role
    ↓
Permission

---

九、Infrastructure

基础设施层。

box-infrastructure

负责：

MySQL
Redis
Elasticsearch
MinIO
HTTP Client
Encryption
Lock
File Storage
Search
Vector Store

例如：

MySQLRepository
RedisService
ElasticsearchService
MinioService

业务模块不要直接操作：

RedisTemplate
MinioClient
ElasticsearchClient

而是通过基础设施抽象。

---

十、领域层

box-domain

存放核心领域对象：

Agent
AgentVersion
Workflow
WorkflowNode
WorkflowEdge
KnowledgeBase
Document
Tool
Model
Conversation
Message
Execution
Trace

这里不应该出现：

Controller
HTTP
Redis
MySQL

核心思想：

«Domain 不依赖 Infrastructure。»

---

十一、Application 层

box-application

负责业务编排。

例如：

CreateAgentService
PublishAgentService
ExecuteAgentService
CreateWorkflowService
ExecuteWorkflowService
UploadDocumentService
SearchKnowledgeService

Application：

Controller
    ↓
Application Service
    ↓
Domain
    ↓
Infrastructure

---

十二、Agent 模块

box-agent

核心职责：

Agent CRUD
Agent Version
Agent Configuration
Prompt
Model
Knowledge
Tool
Memory
Variable
Publish

核心实体：

Agent
AgentVersion
AgentConfig
AgentKnowledge
AgentTool
AgentVariable

---

十三、Agent 数据结构

核心 Agent：

Agent
│
├── id
├── workspace_id
├── name
├── description
├── avatar
├── status
├── draft_version
├── published_version
├── created_by
├── created_at
└── updated_at

版本：

AgentVersion
│
├── id
├── agent_id
├── version
├── system_prompt
├── model_config
├── knowledge_config
├── tool_config
├── memory_config
├── workflow_id
├── variables
├── status
└── created_at

这里非常重要：

«Agent 本身是容器，AgentVersion 才是真正可以运行的配置快照。»

因此运行时：

Agent
 ↓
Published Version
 ↓
Runtime

而不是直接读取 Agent 草稿。

---

十四、Model 模块

box-model

负责：

Provider
Model
Embedding Model
Reranker
API Key
Model Config

结构：

ModelProvider
      │
      ├── Model
      ├── Embedding
      └── Reranker

例如：

OpenAI
├── GPT
└── Embedding

Anthropic
└── Claude

智谱
├── GLM
└── Embedding

DeepSeek
└── DeepSeek

Ollama
├── Qwen
└── Llama

---

十五、LangChain4j 架构

Box 不直接把 LangChain4j 写进 Controller。

错误方式：

@PostMapping("/chat")
public void chat() {
    ChatLanguageModel model = ...;
    model.chat(...);
}

正确方式：

Controller
     ↓
AgentRuntime
     ↓
AgentExecutor
     ↓
PromptBuilder
     ↓
Memory
     ↓
Retriever
     ↓
ToolExecutor
     ↓
LangChain4j
     ↓
LLM Provider

核心接口：

public interface AgentExecutor {

    AgentExecutionResult execute(
        AgentExecutionContext context
    );

    Flux<String> stream(
        AgentExecutionContext context
    );
}

---

十六、Agent Runtime

Agent 对话执行是 Box 核心能力之一。

**主实现模块：`box-agent`**（`AgentChatExecutor` / `AgentChatPreparer`）。`box-runtime` 负责 **Workflow** 节点执行，二者不混用。

运行流程：

Request
  ↓
Authentication
  ↓
Load Agent
  ↓
Load Published Version
  ↓
Create Execution
  ↓
Build Context
  ↓
Build Prompt
  ↓
Load Memory
  ↓
RAG
  ↓
Tool
  ↓
LLM
  ↓
Output
  ↓
Trace
  ↓
Message

完整流程：

                Agent Runtime
                     │
                     ▼
             Load Agent Version
                     │
                     ▼
             Build ExecutionContext
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
       Prompt      Memory      Variable
          │          │          │
          └──────────┼──────────┘
                     ▼
                Knowledge
                     │
                     ▼
                  Tools
                     │
                     ▼
                 LangChain4j
                     │
                     ▼
                    LLM
                     │
                     ▼
                  Output
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
       Message     Trace      Metrics

---

十七、AgentExecutionContext

Runtime 中最重要的数据结构之一：

public class AgentExecutionContext {

    private String executionId;

    private String agentId;

    private String agentVersionId;

    private String conversationId;

    private String userId;

    private Map<String, Object> variables;

    private List<ChatMessage> messages;

    private Map<String, Object> metadata;

    private ExecutionStatus status;
}

它就是一次 Agent 执行过程中的：

«运行时上下文»

Workflow 同样使用 Context。

---

十八、Workflow Engine

Workflow 是 Box 第二个核心。

Workflow
     │
     ├── Nodes
     │
     └── Edges

例如：

Start
  ↓
Input
  ↓
Knowledge Search
  ↓
LLM
  ↓
Condition
  ├── true → HTTP Tool
  │
  └── false → LLM
              ↓
            Output

---

十九、Workflow 数据模型

Workflow：

Workflow
│
├── id
├── workspace_id
├── name
├── description
├── status
├── version
├── created_by
└── updated_at

Node：

WorkflowNode
│
├── id
├── workflow_id
├── node_key
├── node_type
├── name
├── position_x
├── position_y
├── config_json
└── sort_order

Edge：

WorkflowEdge
│
├── id
├── workflow_id
├── source_node_id
├── target_node_id
├── source_port
├── target_port
└── condition

---

二十、Workflow Runtime

核心接口：

public interface WorkflowExecutor {

    WorkflowExecutionResult execute(
        WorkflowExecutionContext context
    );

    Flux<WorkflowEvent> stream(
        WorkflowExecutionContext context
    );
}

执行过程：

Workflow
   ↓
Validate
   ↓
Create Execution
   ↓
Find Start Node
   ↓
Execute Node
   ↓
Get Output
   ↓
Resolve Edge
   ↓
Execute Next Node
   ↓
...
   ↓
Output

---

二十一、Node Executor 设计

不要写：

if (node.getType().equals("LLM")) {
    ...
}

if (node.getType().equals("HTTP")) {
    ...
}

应该采用：

«Strategy + Registry»

设计。

public interface NodeExecutor {

    String nodeType();

    NodeExecutionResult execute(
        NodeExecutionContext context
    );
}

例如：

NodeExecutor
│
├── StartNodeExecutor
├── InputNodeExecutor
├── OutputNodeExecutor
├── LlmNodeExecutor
├── AgentNodeExecutor
├── KnowledgeNodeExecutor
├── HttpNodeExecutor
├── ConditionNodeExecutor
├── SwitchNodeExecutor
├── LoopNodeExecutor
├── CodeNodeExecutor
├── VariableNodeExecutor
└── TemplateNodeExecutor

注册：

NodeExecutorRegistry

运行：

NodeExecutor executor =
    registry.get(node.getNodeType());

executor.execute(context);

这样以后新增：

MCP Node
Image Node
Audio Node
SQL Node
SubWorkflow Node

不需要修改核心 Runtime。

---

二十二、Knowledge 模块

box-knowledge

负责：

Knowledge Base
Document
Document Parser
Chunk
Embedding
Vector Search
Keyword Search
Hybrid Search
Rerank
Citation

RAG：

Document
 ↓
Parse
 ↓
Clean
 ↓
Chunk
 ↓
Embedding
 ↓
Elasticsearch

查询：

Question
 ↓
Query Rewrite
 ↓
Embedding
 ↓
Vector Search
 +
Keyword Search
 ↓
Hybrid Search
 ↓
Rerank
 ↓
Top K
 ↓
Context
 ↓
LLM

---

二十三、为什么 Elasticsearch

V1 不额外引入：

Milvus
Qdrant
Weaviate
PGVector

而采用：

Elasticsearch

统一承担：

全文检索
关键词检索
向量检索
Hybrid Search
Metadata Filter

这样基础设施更简单。

                 Elasticsearch
                /      |       \
               /       |        \
          Keyword    Vector    Hybrid
           Search    Search     Search

MySQL 负责：

业务数据

ES 负责：

检索数据

两者职责严格分离。

---

二十四、Knowledge 数据流

上传：

Browser
   ↓
Spring Boot
   ↓
MinIO
   ↓
Document Record
   ↓
Parser
   ↓
Text
   ↓
Chunk
   ↓
Embedding
   ↓
Elasticsearch

查询：

User Question
      ↓
Embedding
      ↓
ES Vector Search
      +
ES Keyword Search
      ↓
Hybrid
      ↓
Top K
      ↓
Rerank
      ↓
Context

---

二十五、MinIO

MinIO 用于：

PDF
DOCX
TXT
Markdown
Images
Attachments
Avatar
Knowledge Files

建议 Bucket：

box-files
box-knowledge
box-assets
box-avatars

文件路径：

box-knowledge/
    {workspaceId}/
        {knowledgeId}/
            {documentId}/
                original/
                processed/

---

二十六、Tool 模块

box-tool

统一 Tool 抽象：

public interface AgentTool {

    String getName();

    String getDescription();

    ToolSchema getSchema();

    ToolResult execute(
        ToolExecutionContext context
    );
}

Tool 类型：

HTTP
Function
Database
Code
MCP

统一执行：

Agent
 ↓
Tool Registry
 ↓
Tool Permission
 ↓
Tool Executor
 ↓
Tool
 ↓
Result

---

二十七、HTTP Tool

必须重点考虑 SSRF。

禁止：

127.0.0.1
localhost
0.0.0.0
169.254.x.x
内网 IP
Docker 网段
Metadata Service

例如：

http://127.0.0.1
http://localhost
http://192.168.x.x
http://10.x.x.x

默认禁止。

同时限制：

Timeout
Connect Timeout
Read Timeout
Response Size
Redirect
Content Type
Request Header

---

二十八、Database Tool

Database Tool：

Agent
 ↓
Database Tool
 ↓
SQL Validator
 ↓
Permission
 ↓
MySQL
 ↓
Result

V1 建议：

SELECT

作为默认允许操作。

对于：

INSERT
UPDATE
DELETE
DROP
ALTER
TRUNCATE

必须进行高风险控制。

---

二十九、MCP

MCP 作为 Tool 的一种来源：

MCP Server
     ↓
Discover
     ↓
Tools
     ↓
Tool Registry
     ↓
Agent

因此 Agent 不需要关心：

这个 Tool 是 HTTP
还是 MCP
还是 Function

统一：

AgentTool

---

三十、Conversation

Conversation 模块：

Conversation
Message
MessageContent
Attachment

结构：

Conversation
      │
      ├── User Message
      ├── Assistant Message
      ├── Tool Message
      └── System Message

消息内容：

TEXT
MARKDOWN
CODE
IMAGE
FILE
TOOL_CALL
TOOL_RESULT
CITATION

---

三十一、SSE Streaming

聊天默认使用 HTTP + SSE（非 WebSocket）。

**入口**：

- `POST /api/v1/agents/{id}/chat`（`stream=true`）
- `POST /api/v1/conversations/{id}/messages`
- `POST /api/v1/published/agents/{id}/chat`

响应：`Content-Type: text/event-stream`，每帧 `data: {json}\n\n`。

**实现类**：`com.boxai.agent.api.ChatStreamEvent`（`AgentChatExecutor` 发送）

| type | 说明 |
|------|------|
| `citations` | RAG 引用 JSON 数组（流开始前） |
| `delta` | 模型回答文本增量 |
| `tool.start` / `tool.delta` / `tool.end` | Tool 执行生命周期 |
| `tool.confirm` | 危险 Tool 需二次确认 |
| `done` | 流结束（可含 `executionId`） |
| `error` | 错误信息 |

示例：

```json
{"type":"delta","content":"你好"}
{"type":"done","executionId":12345}
```

详细字段见 `06-Runtime.md` §33。

---

三十二、Redis

Redis V1 用于：

JWT Session
Rate Limit
Conversation Cache
Agent Runtime Context
Distributed Lock
API Key Rate Limit
Verification Code
Temporary Data

Key 规范：

box:user:session:{id}

box:rate_limit:{key}

box:agent:runtime:{executionId}

box:conversation:{conversationId}

box:verify:{email}

box:lock:{resource}

TTL 必须明确。

---

三十三、Trace

Trace 是 Box 非常重要的能力。

一次请求：

Agent
 │
 ├── Prompt
 │
 ├── Memory
 │
 ├── Knowledge Search
 │      ├── Vector Search
 │      └── Keyword Search
 │
 ├── Tool
 │
 └── LLM
        └── Output

最终形成：

Trace
 └── Span
      ├── LLM
      ├── RAG
      ├── Tool
      ├── Workflow
      └── Node

每个 Span：

spanId
parentSpanId
type
name
input
output
startTime
endTime
duration
status
error
tokens
metadata

这样调试 Agent 时可以看到：

为什么回答错误？
为什么慢？
哪个 Tool 慢？
哪个 LLM 消耗 Token？
RAG 找到了什么？

---

三十四、Execution 模型

统一：

Execution

支持：

Agent Execution
Workflow Execution
Node Execution
Tool Execution
LLM Execution

状态：

PENDING
RUNNING
SUCCESS
FAILED
CANCELLED
TIMEOUT

结构：

Execution
    │
    ├── Trace
    │
    ├── Agent Execution
    │
    ├── Workflow Execution
    │
    └── Node Execution

---

三十五、Publish

Agent 发布：

Draft
 ↓
Validate
 ↓
Create Version
 ↓
Publish
 ↓
Published

发布渠道：

Web Chat
API
Embed

API：

POST /api/v1/published/{agentId}/chat

认证：

Authorization: Bearer ax-xxxx

---

三十六、API Key

API Key：

ax_live_xxxxxxxxx

数据库不能保存明文。

保存：

key_prefix
key_hash

例如：

key_prefix = ax_live_8f31
key_hash   = SHA-256(...)

完整 Key：

«只在创建时展示一次。»

---

三十七、RBAC

权限模型：

User
 ↓
Workspace Member
 ↓
Role
 ↓
Permission

权限：

agent:create
agent:read
agent:update
agent:delete
agent:publish

workflow:create
workflow:update
workflow:execute

knowledge:create
knowledge:read
knowledge:update

tool:create
tool:execute

workspace:manage
member:manage
api_key:manage

最终权限判断：

User
 ↓
Workspace
 ↓
Role
 ↓
Permission
 ↓
Resource

---

三十八、数据库设计原则

MySQL 不负责：

Vector Search
Full Text Search
Runtime Large Payload
Trace Large Payload

MySQL 主要负责：

用户
工作空间
Agent
Workflow
Knowledge Metadata
Tool
Model
Conversation Metadata
权限
发布
Execution Metadata

ES：

Chunk
Vector
Search Index

MinIO：

File

Redis：

Cache
Session
Temporary Runtime State

---

三十九、核心 MySQL 表

V1 核心表建议：

sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission

workspace
workspace_member

tenant
tenant_member
tenant_usage

agent
agent_version
agent_variable
agent_knowledge
agent_tool

model_provider
model_definition
model_credential

knowledge_base
knowledge_document
knowledge_chunk

tool
tool_http_config
tool_database_config
tool_function_config

workflow
workflow_version
workflow_node
workflow_edge

conversation
message
message_attachment

execution
execution_node
trace
trace_span

publish
api_key

notification
audit_log

大约：

50 张左右

但实际开发时不应该为了“凑 50 张表”而拆表。

---

四十、数据库关系

核心关系：

Workspace
 │
 ├── Agent
 │     └── AgentVersion
 │
 ├── Workflow
 │     └── WorkflowVersion
 │
 ├── KnowledgeBase
 │     └── Document
 │
 ├── Tool
 │
 ├── Conversation
 │     └── Message
 │
 ├── API Key
 │
 └── Members

Agent：

Agent
 │
 ├── Version
 ├── Knowledge
 ├── Tool
 ├── Workflow
 └── Model

---

四十一、包结构

推荐采用：

com.boxai

结构：

com.boxai
│
├── bootstrap
│
├── common
│
├── security
│
├── infrastructure
│
├── domain
│
├── application
│
└── modules
    │
    ├── user
    │
    ├── workspace
    │
    ├── agent
    │
    ├── model
    │
    ├── knowledge
    │
    ├── tool
    │
    ├── workflow
    │
    ├── conversation
    │
    ├── runtime
    │
    ├── publish
    │
    ├── trace
    │
    └── analytics

以 Agent 为例：

agent
├── controller
├── application
│   ├── service
│   └── command
├── domain
│   ├── entity
│   ├── repository
│   └── service
├── infrastructure
│   ├── mapper
│   └── repository
└── dto
    ├── request
    └── response

---

四十二、Frontend 项目结构

**box-web**（C 端）

```
box-web/
├── src/api · components · composables · layouts · router · stores · views
└── vite.config.ts   # alias: @ → src, @box/ui → ../box-ui/src
```

默认入口 `/` → `/chat`；设置子路由在 `/settings/*`；成员管理在 `/team`（非 `/settings/members`）。

**box-admin-web**（平台管理端）

```
box-admin-web/
├── src/views/tenants · plans · platform-models · agent-templates · plugin-catalog · system-config
└── 默认 / → /tenants
```

**box-ui**（共享 UI）

```
box-ui/src/
├── components/   # BrandWordmark、PageHeader 等
├── layouts/      # AppShellLayout、AuthLayout
└── styles/       # theme.css、fonts.css
```

C 端与 admin 通过 `@box/ui` 复用 Layout 与品牌组件。

---

四十三、Agent Builder 前端架构

Agent Builder 是最复杂的页面。

AgentBuilder
│
├── AgentHeader
│
├── AgentSidebar
│
├── AgentConfigPanel
│   ├── PromptPanel
│   ├── ModelPanel
│   ├── KnowledgePanel
│   ├── ToolPanel
│   ├── MemoryPanel
│   ├── VariablePanel
│   └── AdvancedPanel
│
└── PreviewPanel

---

四十四、Workflow Editor

WorkflowEditor
│
├── Toolbar
│
├── NodePanel
│
├── Canvas
│   ├── Nodes
│   └── Edges
│
├── ConfigPanel
│
└── DebugPanel

使用：

Vue Flow

实现：

拖拽
连接
缩放
移动
删除
复制
撤销
重做
自动布局
节点配置
调试

---

四十五、Workflow JSON

前端不要直接依赖几十张数据库表。

前端 Workflow 可以使用：

{
  "nodes": [
    {
      "id": "start",
      "type": "start",
      "position": {
        "x": 100,
        "y": 100
      },
      "config": {}
    },
    {
      "id": "llm_1",
      "type": "llm",
      "position": {
        "x": 400,
        "y": 100
      },
      "config": {
        "model": "deepseek-chat",
        "temperature": 0.7
      }
    }
  ],
  "edges": [
    {
      "source": "start",
      "target": "llm_1"
    }
  ]
}

后端保存时再进行：

JSON
 ↓
Validate
 ↓
Workflow Definition
 ↓
Database

---

四十六、Workflow 校验

运行之前必须：

Validate

检查：

是否存在 Start
是否存在 Output
是否存在孤立节点
是否存在环
节点配置是否合法
引用变量是否存在
Tool 是否有权限
Model 是否存在
Knowledge 是否存在
Workflow 是否存在非法连接

例如：

Start
 ↓
LLM
 ↓
Output

合法。

而：

LLM
 ↓
LLM

如果没有 Start：

INVALID

---

四十七、错误处理

统一异常：

BusinessException
ValidationException
AuthorizationException
ResourceNotFoundException
RuntimeException
ToolExecutionException
ModelExecutionException
KnowledgeException
WorkflowException

统一响应：

{
  "code": "AGENT_NOT_FOUND",
  "message": "Agent 不存在",
  "data": null
}

---

四十八、API 规范

统一：

/api/v1

例如：

GET    /api/v1/agents
POST   /api/v1/agents
GET    /api/v1/agents/{id}
PUT    /api/v1/agents/{id}
DELETE /api/v1/agents/{id}

Workflow：

GET    /api/v1/workflows
POST   /api/v1/workflows
PUT    /api/v1/workflows/{id}
POST   /api/v1/workflows/{id}/debug
POST   /api/v1/workflows/{id}/publish

Chat：

POST /api/v1/agents/{id}/chat

Streaming：

Content-Type: text/event-stream

---

四十九、完整运行架构

最终一次用户请求：

                    User
                     │
                     ▼
                 Vue 3 Web
                     │
                     ▼
                  Nginx
                     │
                     ▼
               Spring Boot
                     │
                     ▼
               API Controller
                     │
                     ▼
             AgentApplication
                     │
                     ▼
                AgentRuntime
                     │
        ┌────────────┼─────────────┐
        ▼            ▼             ▼
     Prompt        Memory        Variable
        │            │             │
        └────────────┼─────────────┘
                     ▼
                Knowledge
                     │
                     ▼
               Elasticsearch
                     │
                     ▼
                  Tools
                     │
                     ▼
                LangChain4j
                     │
                     ▼
                    LLM
                     │
                     ▼
                  SSE
                     │
                     ▼
                  Browser

---

五十、完整数据存储架构

                  Box
                    │
       ┌────────────┼────────────┐
       │            │            │
       ▼            ▼            ▼
     MySQL        Redis       MinIO
       │            │            │
       │            │            └── Files
       │            │
       │            └── Cache / Session
       │
       └── Business Data


                    Box
                      │
                      ▼
               Elasticsearch
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Keyword      Vector      Hybrid
       Search       Search      Search

---

五十一、Docker Compose

V1 开发环境（`deploy/docker-compose.yml`）：

**默认 profile（基础设施）**：

```
mysql · redis · elasticsearch · minio
```

**可选 profile `app`**：额外构建并启动 `box-server`（8080），依赖上述中间件。

```bash
cd deploy
docker compose up -d                              # 仅中间件
docker compose --profile app up -d --build        # 中间件 + box-server
```

**本地进程**：

- `box-web`：`npm run dev`（5173，代理 `/api` → 8080）
- `box-admin-web`：独立 dev 端口

不加入：Kafka、Zookeeper、Nacos、Kubernetes。

---

五十二、Docker 网络

box-network

Compose 内服务：

mysql:3306
redis:6379
elasticsearch:9200
minio:9000

本地开发时 `box-server` 默认 `localhost:8080`，`box-web` 经 Vite 代理 `/api` 至后端；生产可经 Nginx 统一入口。

---

五十三、未来扩展路线

V1：

                 Box
                   │
            Modular Monolith
                   │
      ┌────────────┼────────────┐
      ▼            ▼            ▼
    Agent       Workflow     Knowledge

V2：

                   Gateway
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   Agent Service Workflow      Knowledge
        │            │             │
        └────────────┼─────────────┘
                     ▼
                 Runtime

V3：

                    Gateway
                       │
       ┌───────────────┼────────────────┐
       ▼               ▼                ▼
 Agent Service    Workflow Service   Knowledge
       │               │                │
       └───────────────┼────────────────┘
                       ▼
                Distributed Runtime
                       │
                ┌──────┼──────┐
                ▼      ▼      ▼
              Kafka   Redis   K8s

因此 V1 的模块边界必须提前设计好。

---

五十四、核心设计原则

Box 整个项目必须遵守以下原则：

原则 1：Agent 与 Runtime 分离

Agent = 配置
Runtime = 执行

---

原则 2：Draft 与 Published 分离

Draft
 ↓
Version
 ↓
Published

生产环境永远运行：

Published Version

---

原则 3：Workflow 与 Workflow Runtime 分离

Workflow = Definition
Runtime = Execution

---

原则 4：Tool 统一抽象

无论：

HTTP
Database
Function
Code
MCP

最终都实现：

AgentTool

---

原则 5：LLM Provider 统一抽象

Agent 不应该知道：

OpenAI
Claude
DeepSeek
GLM
Ollama

Agent 只知道：

Model

---

原则 6：业务数据与检索数据分离

MySQL
    ↓
Business

Elasticsearch
    ↓
Search / Vector

---

原则 7：文件与数据库分离

MinIO
    ↓
File

MySQL
    ↓
Metadata

---

原则 8：Runtime 必须可观察

每一次执行都应该能够回答：

什么时候开始？
执行了什么？
调用了哪个模型？
用了多少 Token？
搜索了什么？
调用了什么 Tool？
哪个节点失败？
为什么失败？
耗时多少？

---

五十五、最终架构总结

Box V1 最终确定为：

                    ┌─────────────────┐
                    │   Vue 3 + TDesign  │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │      Nginx      │
                    └────────┬────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   Spring Boot 3                         │
│                                                         │
│ User / Workspace / Agent / Model / Knowledge / Tool     │
│ Workflow / Conversation / Publish / Trace / Analytics   │
│                                                         │
│                 ┌─────────────────┐                     │
│                 │  Agent Runtime  │                     │
│                 └────────┬────────┘                     │
│                          │                              │
│                 ┌────────▼────────┐                     │
│                 │ Workflow Runtime│                     │
│                 └────────┬────────┘                     │
│                          │                              │
│                 ┌────────▼────────┐                     │
│                 │   LangChain4j   │                     │
│                 └────────┬────────┘                     │
└──────────────────────────┼──────────────────────────────┘
                           │
          ┌────────────────┼─────────────────┐
          ▼                ▼                 ▼
       ┌──────┐         ┌──────┐        ┌──────────────┐
       │MySQL │         │Redis │        │Elasticsearch │
       └──────┘         └──────┘        └──────────────┘
                                             │
                                             │
                                        Vector / Search

                       ┌──────────┐
                       │  MinIO   │
                       └──────────┘

V1 核心原则：

«Vue 3 + TDesign Vue Next + Java 21 + Spring Boot 3 + LangChain4j + MySQL + Redis + Elasticsearch + MinIO + 模块化单体。»

暂时：

«不使用 Kafka、不使用微服务、不使用 Kubernetes。»

后续需要扩展时，再将 Runtime、Knowledge、Workflow 等模块逐步拆分。