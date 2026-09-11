Box Master Prompt

企业级 AI Agent / Workflow 智能体开发平台

---

1. 项目基本信息

你现在需要负责从 0 到 1 开发一个企业级 AI Agent 开发与运行平台。

项目名称：

Box AI（产品名：Box）

Box 的产品定位类似 Coze、Dify、Flowise 等 AI Agent 平台，但禁止直接复制任何现有产品的 UI、代码、数据库设计或交互设计。

Box 的目标是打造一个：

«面向个人开发者、企业开发团队和普通业务人员的可视化 AI Agent 创建、编排、运行、管理和发布平台。»

平台需要支持：

- AI Agent 创建
- Agent Prompt 配置
- 多模型接入
- Workflow 可视化编排
- 知识库
- RAG
- Tool 工具
- MCP
- Memory
- Conversation
- Agent Runtime
- Workflow Runtime
- Agent 发布
- API 调用
- Web Chat
- Agent 调试
- 执行记录
- Trace
- Analytics
- 多租户
- RBAC 权限
- 模型管理
- API Key 管理
- 文件管理
- 系统管理

最终目标不是制作一个简单 Demo，而是开发成：

«具备真实企业级产品架构、可扩展、可维护、可测试、可部署的 AI Agent SaaS 平台。»

---

2. 核心设计理念

整个系统必须遵循以下原则：

2.1 平台自主控制

Box 是平台本身。

LangChain4j 是 AI 能力基础设施，而不是整个业务系统。

禁止出现：

Box = LangChain4j

正确架构应该是：

                    Box Platform
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
   Agent Runtime     Workflow Runtime   Conversation
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                   Box AI Core
                          │
                    LangChain4j
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
       LLM              RAG              Tool
        │                 │                 │
     OpenAI           Elasticsearch       HTTP
     GLM              Embedding           MCP
     Qwen             Reranker             DB
     DeepSeek
     Ollama

Box 自己负责：

- Agent 生命周期
- Agent 配置
- Agent 版本
- Workflow
- Workflow Runtime
- Node Runtime
- 权限
- 租户
- 发布
- API
- 会话
- Trace
- Analytics
- 业务数据

LangChain4j 负责：

- LLM 调用
- Streaming
- Embedding
- AI Service
- Tool Calling
- Chat Memory 能力
- RAG 相关 AI 能力
- 模型适配

---

3. 技术栈

3.1 Frontend

必须使用：

Vue 3
TypeScript
Vite
Vue Router
Pinia
Axios
TDesign Vue Next（tdesign-vue-next）
tdesign-icons-vue-next
Vue Flow
ECharts

禁止：

Semi Design Vue / Semi UI / 其他 Semi 组件包

前端组件选型、Props、插槽、DOM 结构、图标名称：

以 TDesign MCP 为准（framework = vue-next）。
不要凭记忆编造 TDesign API。

推荐：

VueUse
Zod
Day.js

Frontend 主要负责：

- 管理后台
- Agent Builder
- Workflow Editor
- Agent Debug
- Knowledge Base
- Tool Management
- MCP Management
- Model Management
- Conversation
- Analytics
- System Settings

---

4. Backend 技术栈

必须使用：

Java 21
Spring Boot 3
LangChain4j
MyBatis-Flex
Spring Security
JWT
Bean Validation
Jackson
Lombok

后端架构采用：

«模块化单体 Modular Monolith»

第一阶段：

一个 Spring Boot 应用
+
多个业务模块

不要一开始就拆成微服务。

---

5. Database

5.1 MySQL

使用：

MySQL 8.x

MySQL 是：

«Box 核心业务数据库。»

存储：

- User
- Tenant
- Workspace
- Role
- Permission
- Agent
- AgentVersion
- Workflow
- WorkflowVersion
- Conversation
- ConversationMessage
- Tool
- MCP
- Model
- KnowledgeBase
- KnowledgeDocument
- Publish
- API Key
- Execution
- Trace
- Analytics
- Task

---

6. Redis

Redis 用于：

缓存
Session
Rate Limit
Distributed Lock
临时执行状态
短期会话缓存
验证码
API 限流
Agent Runtime 临时状态
Workflow Runtime 临时状态

必须合理设置 TTL。

禁止把所有业务数据都放入 Redis。

---

7. Elasticsearch

Elasticsearch 用于：

全文搜索
向量搜索
Hybrid Search
知识库检索
日志检索
Trace 查询

知识库推荐：

MySQL
    ↓
文档元数据

MinIO
    ↓
原始文件

Elasticsearch
    ↓
Chunk
Embedding
全文索引
Vector

---

8. MinIO

MinIO 用于文件对象存储。

例如：

PDF
DOCX
TXT
Markdown
HTML
图片
附件
用户头像
Agent 文件
知识库原始文件

MySQL 只保存：

bucket
objectKey
fileName
contentType
size
hash
status

禁止把大型二进制文件直接存储到 MySQL。

---

9. Kafka

当前版本禁止使用 Kafka。

第一阶段不引入：

Kafka
RocketMQ
RabbitMQ

知识库异步处理、任务执行等场景优先使用：

Spring TaskExecutor
@Async
Scheduled Task
Redis
MySQL Task Table

例如：

用户上传文件
       ↓
创建 ingestion_task
       ↓
Spring Async
       ↓
解析
       ↓
清洗
       ↓
Chunk
       ↓
Embedding
       ↓
Elasticsearch
       ↓
完成

未来当系统达到大规模并发后，再考虑引入 Kafka。

---

10. AI 核心框架

必须使用：

«LangChain4j»

禁止在 Java 后端使用 Python LangChain 作为核心 AI 框架。

---

11. LangChain4j 使用原则

LangChain4j 应该作为：

«AI Integration Layer»

而不是：

«Business Architecture»

例如：

Box AgentRuntime
        ↓
Box AI Core
        ↓
LangChain4j Adapter
        ↓
ChatLanguageModel

而不是：

Controller
 ↓
LangChain4j
 ↓
所有业务逻辑

---

12. AI Model 抽象

Box 必须自己设计统一模型抽象。

例如：

public interface LlmProvider {

    ChatResponse chat(ChatRequest request);

    Flux<ChatResponse> stream(ChatRequest request);
}

具体模型：

OpenAI
GLM
DeepSeek
Qwen
Ollama
OpenAI Compatible
Custom

统一进入：

ModelProvider
      ↓
LlmClient
      ↓
LangChain4j

业务层禁止直接依赖具体厂商 SDK。

---

13. Embedding

Embedding 必须统一抽象。

例如：

EmbeddingService

支持：

OpenAI
Qwen
GLM
Ollama
Custom

用于：

Knowledge RAG
Semantic Search
Memory

---

14. Agent Runtime

Agent Runtime 是整个系统的核心。

必须由 Box 自己实现。

主要负责：

Agent Config
Prompt
Model
Memory
Knowledge
Tool
Workflow
Execution
Streaming
Trace
Error Handling

执行过程：

User Request
      ↓
Agent Runtime
      ↓
Load Agent Version
      ↓
Build Execution Context
      ↓
Load Model
      ↓
Load Prompt
      ↓
Load Knowledge
      ↓
Load Memory
      ↓
Load Tools
      ↓
LangChain4j
      ↓
LLM
      ↓
Tool / RAG / Memory
      ↓
Final Response

---

15. Workflow Runtime

Workflow Runtime 也必须由 Box 自己实现。

禁止完全依赖第三方 Workflow Engine。

---

16. Workflow 数据结构

Workflow 基本结构：

Workflow
 ├── Node
 ├── Edge
 ├── Variable
 ├── Input
 ├── Output
 └── Metadata

例如：

{
  "nodes": [],
  "edges": [],
  "variables": []
}

---

17. Workflow Node

第一阶段至少支持：

Start
Input
Output
LLM
Knowledge
HTTP
Tool
Condition
Loop
Variable
Template
Code
Delay

未来支持：

Agent
MCP
Human Approval
Database
Webhook
Sub Workflow
Parallel
Merge

---

18. Workflow Runtime 设计

必须使用：

Strategy Pattern
Registry Pattern
Factory Pattern

例如：

public interface NodeExecutor {

    NodeType support();

    NodeExecutionResult execute(
        NodeExecutionContext context
    );
}

然后：

NodeRegistry
     ↓
NodeType
     ↓
NodeExecutor

禁止出现：

if(node.type == ...)
else if(node.type == ...)
else if(...)

这种巨型代码。

---

19. Workflow Execution

执行流程：

Workflow JSON
      ↓
Validation
      ↓
Compile
      ↓
Execution Plan
      ↓
Workflow Runtime
      ↓
Node Executor
      ↓
Context
      ↓
Next Node
      ↓
Result

必须支持：

变量
上下文
条件分支
循环
超时
重试
错误处理
节点跳过
节点失败
执行取消
执行状态
Trace

未来支持：

暂停
恢复
人工审批
并行节点
子 Workflow

---

20. Execution Context

Workflow Runtime 必须存在统一执行上下文：

public class ExecutionContext {

    private String traceId;

    private String executionId;

    private String agentId;

    private String workflowId;

    private Map<String, Object> variables;

    private Map<String, Object> outputs;

    private Map<String, Object> metadata;
}

所有 Node 都从 Context 中获取输入，并向 Context 写入结果。

---

21. RAG 架构

完整流程：

Upload
 ↓
MinIO
 ↓
Document Parser
 ↓
Text Cleaning
 ↓
Chunking
 ↓
Embedding
 ↓
Elasticsearch
 ↓
Retriever
 ↓
Reranker
 ↓
Context
 ↓
LLM

必须支持：

PDF
DOCX
TXT
Markdown
HTML

---

22. Knowledge Base

知识库必须支持：

创建知识库
删除知识库
编辑知识库
上传文档
删除文档
重新解析
重新向量化
Chunk 查看
搜索测试
RAG 测试
文档状态
处理进度

文档状态：

UPLOADING
PENDING
PARSING
CHUNKING
EMBEDDING
INDEXING
READY
FAILED

---

23. Chunk

Chunk 必须保存：

documentId
chunkId
content
metadata
page
position
tokenCount
embedding

Metadata 至少包括：

source
document
page
chunk

这样最终 AI 回答可以返回：

引用来源
文档
页码
Chunk
相关度

---

24. Tool 系统

Tool 是 Box 的核心能力之一。

统一设计：

ToolDefinition
ToolExecutor
ToolRegistry

例如：

public interface ToolExecutor {

    ToolResult execute(
        ToolContext context,
        Map<String, Object> arguments
    );
}

---

25. Tool 类型

第一阶段：

HTTP Tool
Function Tool
Database Tool
Code Tool
Custom Tool
MCP Tool

---

26. HTTP Tool 安全

HTTP Tool 必须重点防御：

SSRF
内网访问
localhost
127.0.0.1
0.0.0.0
私有 IP
Link Local
Cloud Metadata
危险端口
危险协议
恶意 Redirect

禁止 Agent 任意访问内部网络。

必须：

URL Validation
DNS Validation
IP Validation
Protocol Validation
Port Validation
Redirect Validation
Timeout
Rate Limit

---

27. MCP

Box 必须支持 MCP。

架构：

MCP Server
     ↓
MCP Client
     ↓
Tool Discovery
     ↓
Tool Registry
     ↓
Agent Runtime

MCP 工具必须转换成 Box 统一：

ToolDefinition

这样 Agent Runtime 不需要关心：

普通 Tool
MCP Tool

---

28. Memory

Memory 与 Knowledge 必须严格分离。

Knowledge：

«企业知识、文档、资料。»

Memory：

«用户与 Agent 的长期交互信息。»

Memory 分为：

Conversation Memory
Short Term Memory
Long Term Memory
User Memory

---

29. Conversation

Conversation 模块负责：

Conversation
ConversationMessage
Message
Attachment
Citation
ToolCall
TokenUsage

消息类型：

SYSTEM
USER
ASSISTANT
TOOL

必须支持：

SSE Streaming
非流式
停止生成
重新生成
消息重试
消息引用
工具调用记录
Token 统计

---

30. SSE

AI Streaming 必须使用：

SSE

例如：

POST /api/v1/runtime/agents/{agentId}/chat

支持：

stream=true

返回：

token
tool_call
citation
metadata
done
error

---

31. Agent

Agent 至少包含：

名称
描述
Avatar
System Prompt
Model
Temperature
Max Tokens
Knowledge
Tools
Memory
Workflow
Variables
Conversation Settings
Advanced Settings

---

32. Agent Version

Agent 必须支持版本管理。

状态：

DRAFT
TESTING
PUBLISHED
ARCHIVED

生产环境发布后：

«已发布版本不可直接修改。»

修改必须：

Published Version
       ↓
Create New Version
       ↓
Edit
       ↓
Test
       ↓
Publish

---

33. Model Provider

模型管理支持：

OpenAI
GLM
DeepSeek
Qwen
Ollama
OpenAI Compatible
Custom

模型配置：

Provider
Model
Base URL
API Key
Temperature
Max Tokens
Context Window
Embedding Model
Reranker

API Key 必须：

«加密存储。»

禁止：

明文日志
前端直接返回
异常堆栈打印
Trace 输出

---

34. Multi-Tenant

系统必须支持多租户。

结构：

User
 ↓
Tenant
 ↓
Workspace
 ↓
Agent

核心业务表必须考虑：

tenant_id
workspace_id

必须严格实现：

«Tenant Isolation»

禁止用户通过修改 ID 查询其他租户的数据。

---

35. RBAC

角色：

SUPER_ADMIN
TENANT_ADMIN
DEVELOPER
MEMBER

权限：

agent:create
agent:read
agent:update
agent:delete

workflow:create
workflow:update
workflow:execute

knowledge:create
knowledge:update

tool:create
tool:execute

model:manage

user:manage
tenant:manage

使用：

Spring Security
JWT
RBAC

---

36. Authentication

使用：

JWT Access Token
JWT Refresh Token
BCrypt
Spring Security

登录流程：

Login
 ↓
Verify User
 ↓
Generate Access Token
 ↓
Generate Refresh Token
 ↓
Redis / DB

必须支持：

登录
注册
退出
刷新 Token
修改密码
验证码
登录限流

---

37. API Design

统一：

/api/v1

例如：

/api/v1/auth/login

/api/v1/agents

/api/v1/agents/{id}

/api/v1/workflows

/api/v1/knowledge-bases

/api/v1/tools

/api/v1/models

/api/v1/conversations

/api/v1/runtime/agents/{id}/chat

---

38. Controller

Controller 必须保持轻量。

错误：

Controller
 ↓
大量业务逻辑
 ↓
数据库
 ↓
AI

正确：

Controller
 ↓
Application Service
 ↓
Domain
 ↓
Repository
 ↓
Infrastructure

---

39. DTO

禁止直接将数据库 Entity 返回给前端。

必须区分：

Request DTO
Response DTO
Command
Query
Entity
VO

---

40. Exception

统一异常体系：

BizException
SystemException
ValidationException
AuthenticationException
AuthorizationException

统一：

ErrorCode
GlobalExceptionHandler

返回：

{
  "code": "AGENT_NOT_FOUND",
  "message": "Agent not found",
  "traceId": "xxx"
}

---

41. Observability

必须建立 Trace 系统。

结构：

Trace
 ├── Agent Span
 ├── Workflow Span
 ├── Node Span
 ├── LLM Span
 ├── RAG Span
 └── Tool Span

记录：

traceId
executionId
latency
model
tokens
input
output
error
tool
node
metadata

敏感数据必须脱敏。

---

42. Analytics

统计：

Agent 调用次数
用户数量
Conversation 数量
Message 数量
Token
Latency
Error Rate
Tool Call
Workflow Execution
Knowledge Query

未来：

Cost
Quota
Billing

---

43. Rate Limit

Redis 实现：

Login Rate Limit
API Rate Limit
Agent Rate Limit
Tool Rate Limit
Workflow Rate Limit

防止：

暴力请求
API Abuse
Agent Abuse
Tool Abuse

---

44. Prompt Injection

Box 必须考虑：

Prompt Injection
Indirect Prompt Injection
Tool Injection
Data Exfiltration
Jailbreak

尤其是：

Knowledge → LLM
Tool → LLM
MCP → LLM

所有外部数据都必须视为：

«Untrusted Data»

不能默认信任知识库文档中的指令。

---

45. Frontend UI

整体设计风格：

«Modern AI SaaS»

参考设计语言：

ChatGPT
Linear
Vercel
Apple
现代 SaaS

但：

«禁止直接复制这些产品。»

视觉要求：

简洁
高级
专业
克制
高信息密度
优秀的排版
清晰的层级
细腻的交互

避免：

廉价渐变
大量发光
赛博朋克
花哨动画
过度圆角
颜色堆砌

---

46. Design System

基础组件使用：

TDesign Vue Next（tdesign-vue-next）

布局与反馈优先使用 TDesign 已有能力：

Layout / Menu / Breadcrumb / Button / Form / Input / Select / Table /
Dialog / Drawer / Message / Notification / Tabs / Dropdown / Tree / Upload

禁止 Semi Design Vue。

实现页面前先用 TDesign MCP 核对组件文档（framework = vue-next）。

但禁止直接使用默认 Demo 样式。

需要建立：

Box Design System

包括：

Color
Typography
Spacing
Radius
Shadow
Button
Input
Modal
Drawer
Table
Card
Tabs
Dropdown
Toast
Code Editor
Node Editor
Chat UI

支持：

Light
Dark

---

47. Agent Builder

Agent Builder 采用：

Left
Center
Right

结构。

左侧：

Agent Navigation

中间：

Agent Configuration

右侧：

Live Preview

主要配置：

Basic
Prompt
Model
Knowledge
Tools
Memory
Workflow
Advanced

顶部：

Save
Test
Debug
Publish
Version

---

48. Workflow Editor

使用：

«Vue Flow»

布局：

Top Toolbar
Left Node Library
Center Canvas
Right Configuration

顶部：

Save
Run
Debug
Publish
Undo
Redo
Zoom

左侧：

Start
Input
LLM
Knowledge
Tool
HTTP
Condition
Loop
Variable
Template
Code
Delay

中央：

«Workflow Canvas»

右侧：

«Node Configuration»

必须支持：

Drag
Drop
Connect
Delete
Copy
Paste
Multi Select
Zoom
Minimap
Undo
Redo
Keyboard Shortcut

---

49. Agent Debug

必须拥有独立调试面板。

显示：

User Input
Prompt
Model
Knowledge
Tool
Workflow
Token
Latency
Trace
Final Output

例如：

Agent
 ↓
Prompt
 ↓
Knowledge Search
 ↓
LLM
 ↓
Tool
 ↓
LLM
 ↓
Response

用户能够看到整个执行过程。

---

50. Project Structure

Backend：

box-server
├── box-bootstrap
├── box-common
├── box-security
├── box-infrastructure
├── box-domain
├── box-application
└── box-modules
    ├── box-user
    ├── box-workspace
    ├── box-agent
    ├── box-model
    ├── box-knowledge
    ├── box-tool
    ├── box-workflow
    ├── box-conversation
    ├── box-runtime
    ├── box-publish
    ├── box-trace
    └── box-analytics

包名统一：com.boxai.*

---

51. Common Module

包含：

Result
PageResult
ErrorCode
BizException
Utils
Constants
JSON
Security
Trace

---

52. Agent Module

负责：

Agent CRUD
Agent Version
Agent Config
Agent Publish
Agent Settings

---

53. Workflow Module

负责：

Workflow CRUD
Workflow Version
Workflow JSON
Node
Edge
Variable
Validation
Compilation

---

54. Runtime Module

负责：

Agent Runtime
Workflow Runtime
Execution Context
Execution
Streaming
Tool Calling
Memory
RAG
Trace

这是整个系统最核心的模块之一。

---

55. RAG Module

负责：

Document Parser
Chunking
Embedding
Retriever
Reranker
Hybrid Search
Citation

---

56. Tool Module

负责：

Tool Definition
Tool Registry
Tool Executor
HTTP Tool
Function Tool
Database Tool
Code Tool

---

57. MCP Module

负责：

MCP Client
Server Configuration
Tool Discovery
Tool Registration
Connection Management

---

58. Conversation Module

负责：

Conversation
Message
Attachment
Streaming
Citation
Token
Tool Call

---

59. Publish Module

支持：

Web Chat
API
Embed
API Key

未来：

Webhook
第三方平台

---

60. API Runtime

Agent 发布之后：

POST
/api/v1/runtime/agents/{agentId}/chat

可以：

JSON
SSE

例如：

{
  "conversationId": "xxx",
  "message": "你好",
  "stream": true
}

---

61. Security

必须重点考虑：

SQL Injection
XSS
CSRF
SSRF
Prompt Injection
Tool Injection
API Abuse
Privilege Escalation
Tenant Isolation
JWT Security
File Upload Security
MCP Security

文件上传必须检查：

File Extension
Content Type
File Size
File Signature
Malicious File

---

62. Code Quality

必须遵循：

SOLID
DRY
KISS
YAGNI
DDD
Clean Architecture
Clean Code

核心代码必须：

低耦合
高内聚
可测试
可扩展
可维护

---

63. Design Pattern

合理使用：

Strategy
Factory
Registry
Template Method
Builder
Adapter
Decorator
Chain of Responsibility
Observer
Command

禁止为了设计模式而设计模式。

---

64. Logging

日志必须包含：

traceId
requestId
userId
tenantId
executionId

禁止输出：

API Key
JWT
Password
Secret
完整敏感用户信息

---

65. Testing

必须建立：

Unit Test
Integration Test
API Test
Workflow Test
Agent Runtime Test
RAG Test
Tool Test
MCP Test
Security Test
Tenant Isolation Test
Permission Test

核心 Runtime 必须具备高覆盖率。

---

66. Docker

开发环境使用：

Docker Compose

服务：

MySQL
Redis
Elasticsearch
MinIO
Box Backend
Box Frontend

当前：

«不加入 Kafka。»

---

67. Kubernetes

Kubernetes 不作为第一阶段开发目标。

未来可以：

Docker Compose
      ↓
Kubernetes
      ↓
Service
      ↓
Deployment
      ↓
Ingress
      ↓
HPA

因此代码架构必须：

«为未来容器化、水平扩展和微服务化保留空间。»

---

68. Development Strategy

禁止一次性生成整个项目。

必须按照：

开发
 ↓
编译
 ↓
测试
 ↓
修复
 ↓
提交
 ↓
下一阶段

推进。

---

69. 第一阶段

项目初始化：

Vue 3
Spring Boot 3
Java 21
MyBatis-Flex
MySQL
Redis
Elasticsearch
MinIO
LangChain4j
Docker Compose

确保：

前端可以运行
后端可以运行
数据库可以连接
Redis 可以连接
ES 可以连接
MinIO 可以连接
LangChain4j 可以调用模型

---

70. 第二阶段

实现：

User
Tenant
Workspace
Role
Permission
JWT
Login
Register
RBAC

---

71. 第三阶段

实现：

Model Provider
Model
LLM
Streaming
LangChain4j Integration

---

72. 第四阶段

实现：

Agent
Agent Version
Prompt
Model
Agent Configuration
Agent Debug

---

73. 第五阶段

实现：

Conversation
Message
SSE
Conversation Memory

---

74. 第六阶段

实现：

Agent Runtime
Tool Calling
Memory
Trace

---

75. 第七阶段

实现：

Workflow Editor
Vue Flow
Node
Edge
Variable

---

76. 第八阶段

实现：

Workflow Runtime
Node Executor
Condition
Loop
Retry
Timeout
Error Handling

---

77. 第九阶段

实现：

Knowledge Base
Document
MinIO
Parser
Chunk
Embedding
Elasticsearch
RAG
Citation

---

78. 第十阶段

实现：

Tool
HTTP Tool
Function Tool
Database Tool
Code Tool
Tool Permission
Security

---

79. 第十一阶段

实现：

MCP
MCP Client
MCP Server Configuration
Tool Discovery
MCP Tool

---

80. 第十二阶段

实现：

Memory
Long Term Memory
User Memory
Conversation Memory

---

81. 第十三阶段

实现：

Publish
Web Chat
API
API Key
Embed

---

82. 第十四阶段

实现：

Observability
Trace
Execution
Debug
Analytics

---

83. 第十五阶段

实现：

Security Hardening
Rate Limit
Audit
Tenant Isolation
Permission
SSRF
Prompt Injection Protection
File Security

---

84. 第十六阶段

实现：

Docker
Docker Compose
Production Config
Environment Config
Health Check
Monitoring

---

85. Agent Runtime 核心要求

这是整个项目最重要的部分之一。

必须保证：

Agent Runtime

与：

LangChain4j

解耦。

例如：

AgentRuntime
      ↓
AgentExecutionService
      ↓
AiOrchestrator
      ↓
LlmService
      ↓
LangChain4j

未来即使更换 AI 框架：

LangChain4j
↓
其他 AI Framework

也不能导致整个业务架构重写。

---

86. AI Orchestrator

建议建立：

AiOrchestrator

统一协调：

LLM
RAG
Tool
Memory
Workflow

例如：

public interface AiOrchestrator {

    AgentExecutionResult execute(
        AgentExecutionContext context
    );
}

---

87. Tool Execution

Tool 执行必须经过：

Permission
 ↓
Validation
 ↓
Security
 ↓
Rate Limit
 ↓
Timeout
 ↓
Executor
 ↓
Result
 ↓
Trace

不能让 LLM 直接执行危险操作。

---

88. Workflow 与 Agent 关系

Agent 可以：

直接调用 LLM

也可以：

Agent
 ↓
Workflow
 ↓
多个 Node

Workflow 也可以：

LLM
Knowledge
Tool
HTTP
Condition
Loop

最终：

Agent
   ↓
Runtime
   ↓
Workflow
   ↓
Node

---

89. 数据一致性

核心业务操作必须考虑：

Transaction
Idempotency
Optimistic Lock
Version
State Machine

特别是：

Agent Publish
Workflow Publish
Execution
Knowledge Ingestion

---

90. Versioning

Agent 和 Workflow 必须独立版本化：

AgentVersion
WorkflowVersion

版本状态：

DRAFT
TESTING
PUBLISHED
ARCHIVED

发布版本必须不可变。

---

91. Future Scalability

虽然当前使用：

«Modular Monolith»

但必须保证未来可以拆分：

Agent Service
Workflow Service
Knowledge Service
Runtime Service
Model Service
Tool Service
Conversation Service
Analytics Service

因此：

«模块之间必须通过清晰接口通信。»

不要产生严重的模块循环依赖。

---

92. 禁止事项

开发过程中禁止：

禁止 Spring AI
禁止 Python LangChain 作为 Java 后端核心
禁止 Kafka 第一阶段接入
禁止一开始微服务化
禁止 Controller 堆业务逻辑
禁止 Entity 直接返回
禁止明文保存 API Key
禁止 Tool 无权限执行
禁止 HTTP Tool 无 SSRF 防护
禁止 Agent Runtime 直接依赖具体模型厂商
禁止 Workflow 使用巨型 if/else
禁止复制 Coze UI
禁止生成无法运行的伪代码
禁止大量 TODO 冒充功能完成

---

93. AI Coding Agent 工作方式

你作为代码生成 Agent，在开发 Box 时必须遵循：

先理解架构
 ↓
检查当前代码
 ↓
确认模块
 ↓
设计方案
 ↓
修改代码
 ↓
运行测试
 ↓
修复问题
 ↓
验证

禁止：

«不看项目结构就大量创建文件。»

---

94. 每次开发任务要求

每次实现功能前必须明确：

1. 功能目标
2. 影响模块
3. 数据库变化
4. API 变化
5. 核心类
6. 业务流程
7. 异常场景
8. 安全风险
9. 测试方案

---

95. 输出代码要求

生成代码时必须：

完整
可运行
可编译
符合项目现有结构

禁止：

伪代码
省略关键代码
...
TODO

除非明确说明：

«当前只是架构示例。»

---

96. 注释要求

核心代码必须拥有清晰注释。

特别是：

Agent Runtime
Workflow Runtime
RAG
Tool
MCP
Memory
AI Orchestrator

注释必须解释：

«为什么这么设计。»

而不是只解释：

«这行代码做了什么。»

---

97. 数据库规范

数据库必须：

统一命名
统一时间字段
统一主键策略
统一软删除策略
统一索引规范

推荐：

id
tenant_id
workspace_id
created_by
created_at
updated_at
deleted
version

核心查询：

«禁止无条件 SELECT *。»

必须合理设计：

Index
Unique Index
Composite Index
Foreign Key Strategy

---

98. API 返回规范

统一：

{
  "code": 0,
  "message": "success",
  "data": {}
}

分页：

{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "pageSize": 20
  }
}

---

99. 最终产品结构

最终 Box 应形成：

                    Box
                       │
      ┌────────────────┼────────────────┐
      │                │                │
    Agent            Workflow         Knowledge
      │                │                │
      └────────────────┼────────────────┘
                       │
                 Agent Runtime
                       │
              ┌────────┼────────┐
              │        │        │
             LLM      RAG      Tool
              │        │        │
              │        │        ├── HTTP
              │        │        ├── DB
              │        │        ├── Function
              │        │        └── MCP
              │        │
              └────────┼────────┘
                       │
                  LangChain4j
                       │
       ┌───────────────┼────────────────┐
       │               │                │
    OpenAI            GLM             Qwen
    DeepSeek          Ollama          Custom

---

100. 最终开发目标

最终用户应该可以完成：

注册
 ↓
创建 Workspace
 ↓
创建 Agent
 ↓
配置 Prompt
 ↓
选择模型
 ↓
绑定知识库
 ↓
绑定 Tools
 ↓
配置 Memory
 ↓
设计 Workflow
 ↓
测试 Agent
 ↓
查看 Trace
 ↓
发布 Agent
 ↓
获得 API Key
 ↓
通过 API 调用

最终形成完整闭环：

Create
 ↓
Configure
 ↓
Build
 ↓
Debug
 ↓
Test
 ↓
Publish
 ↓
Run
 ↓
Observe
 ↓
Analyze
 ↓
Optimize

---

101. 最终架构原则

请始终牢记：

«Box 是一个 AI Agent Platform，而不是一个简单的 LLM Chat 项目。»

核心竞争力不是：

调用一次 GPT

而是：

Agent
+
Workflow
+
RAG
+
Tool
+
MCP
+
Memory
+
Runtime
+
Publishing
+
Observability
+
Multi-Tenant

其中：

«Agent Runtime + Workflow Runtime + Tool System + RAG + Model Abstraction 是整个系统的核心技术壁垒。»

LangChain4j 负责连接 AI 能力，但：

«Box 必须拥有自己的 Runtime、业务模型、工作流引擎、权限体系、发布体系和可观测体系。»

所有后续开发都必须以本 Master Prompt 作为最高级项目架构约束。

后续开发过程中，如果发现某个技术方案与本架构冲突：

«优先保持整体架构一致性，而不是为了快速实现功能破坏核心设计。»