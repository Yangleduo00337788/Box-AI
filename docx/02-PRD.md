Box PRD

企业级 AI Agent / Workflow 智能体开发平台

文档版本：V1.0

产品名称：Box

产品定位：企业级 AI Agent 创建、编排、调试、发布与运行平台

---

1. 产品概述

1.1 产品定位

Box 是一个类似 Coze / Dify 的 AI Agent 开发平台。

用户无需编写大量 AI 基础代码，即可通过可视化界面：

创建 Agent
    ↓
配置 Prompt
    ↓
选择模型
    ↓
接入知识库
    ↓
配置 Tools
    ↓
配置 Memory
    ↓
编排 Workflow
    ↓
调试
    ↓
发布
    ↓
API / Web Chat 调用

---

2. 产品目标

Box V1 的核心目标：

P0

必须实现：

- 用户注册/登录
- Workspace
- Agent
- Agent 配置
- LLM 模型
- Conversation
- Streaming Chat
- Knowledge Base
- RAG
- Tool
- Workflow
- Workflow Runtime
- Agent Runtime
- Debug
- Publish
- API Key
- RBAC
- Trace
- 基础 Analytics

P1

后续实现：

- MCP
- Long-term Memory
- 多 Agent
- Sub Workflow
- Webhook
- Plugin
- Marketplace

P2

未来实现：

- Billing
- Quota
- Team Collaboration
- Enterprise SSO
- Kubernetes
- 分布式 Runtime
- Marketplace

---

3. 用户角色

3.1 Super Admin

系统管理员。

权限：

用户管理
租户管理
模型管理
系统配置
工具管理
MCP 管理
系统日志
Analytics

---

3.2 Tenant Admin

企业管理员。

权限：

Workspace
成员
角色
Agent
Knowledge
Tool
Model
API Key
Analytics

---

3.3 Developer

开发人员。

权限：

创建 Agent
编辑 Agent
创建 Workflow
创建 Knowledge
配置 Tool
调试
发布
查看 Trace

---

3.4 Member

普通成员。

权限：

使用 Agent
查看授权资源
Conversation

---

4. 产品整体信息架构

Box
│
├── Dashboard
│
├── Agents
│   ├── My Agents
│   ├── Shared
│   └── Templates
│
├── Workflows
│
├── Knowledge
│
├── Tools
│
├── MCP
│
├── Models
│
├── Conversations
│
├── Analytics
│
└── Settings
    ├── Workspace
    ├── Members
    ├── Roles
    ├── API Keys
    └── Security

---

5. 登录注册

5.1 登录页面

页面：

Logo
Box

Welcome back

Email
Password

Remember me

Sign In

Forgot password?

Don't have an account?
Create account

支持：

Email + Password

未来：

GitHub
Google
企业 SSO

---

6. 注册

字段：

Email
Password
Confirm Password
Verification Code

校验：

Email 格式
密码长度
密码强度
验证码
重复注册

注册成功：

Create User
 ↓
Create Tenant
 ↓
Create Workspace
 ↓
Assign TENANT_ADMIN
 ↓
Dashboard

---

7. Dashboard

Dashboard 是用户登录后的首页。

顶部：

Workspace Selector
Search
Notification
Help
User Avatar

核心统计：

Total Agents
Total Conversations
Total Tokens
Total Executions
Success Rate

---

7.1 快速操作

Create Agent
Create Workflow
Create Knowledge Base
Add Model

---

7.2 最近使用

显示：

Recent Agents
Recent Workflows
Recent Conversations

---

7.3 Analytics

显示：

Agent Calls
Token Usage
Execution Count
Error Rate
Latency

时间范围：

Today
7 Days
30 Days
Custom

---

8. Agent 管理

8.1 Agent 列表

字段：

Avatar
Name
Description
Model
Status
Version
Updated At
Owner
Actions

操作：

Open
Edit
Duplicate
Publish
Archive
Delete

---

9. 创建 Agent

点击：

Create Agent

弹窗：

Agent Name
Description
Avatar

创建后进入：

«Agent Builder»

---

10. Agent Builder

这是 Box 最重要的页面之一。

采用：

┌─────────────────────────────────────────┐
│ Header                                  │
├──────────────┬────────────────┬─────────┤
│              │                │         │
│ Navigation   │ Configuration  │ Preview │
│              │                │         │
│              │                │         │
└──────────────┴────────────────┴─────────┘

---

11. Agent Builder 左侧导航

Overview
Prompt
Model
Knowledge
Tools
Memory
Workflow
Variables
Advanced

---

12. Overview

配置：

Agent Name
Description
Avatar
Tags
Visibility

Visibility：

Private
Workspace
Public

---

13. Prompt

Prompt 编辑器。

支持：

System Prompt
Variables
Prompt Templates

例如：

You are {{agent_name}}.

Your role is:
{{role}}

Knowledge:
{{knowledge}}

变量可以来自：

System
User
Conversation
Workflow
Custom

---

14. Model

配置：

Provider
Model
Temperature
Top P
Max Tokens
Stream

高级：

Frequency Penalty
Presence Penalty
Stop Sequences
Context Window

---

15. Knowledge

Agent 可以绑定多个知识库。

页面：

Knowledge Base
Search
Add Knowledge Base

配置：

Top K
Score Threshold
Retrieval Mode
Reranker
Citation

Retrieval Mode：

Vector
Keyword
Hybrid

---

16. Tools

Agent 可以绑定 Tool。

列表：

Tool Name
Description
Type
Status
Permission

支持：

HTTP
Function
Database
Code
MCP

---

17. Tool 权限

每个 Agent 的 Tool 必须独立授权。

例如：

Weather API
✓ Enabled

Search API
✓ Enabled

Database
✗ Disabled

危险工具必须二次确认。

---

18. Memory

配置：

Enable Memory
Conversation Memory
Long Term Memory
User Memory

参数：

Memory Size
Retention
Summarization

---

19. Workflow

Agent 可以绑定 Workflow。

选择：

None
Workflow A
Workflow B
Workflow C

Workflow 可以成为 Agent 的执行流程。

---

20. Variables

支持：

String
Number
Boolean
Object
Array

字段：

Name
Type
Default Value
Required
Description

---

21. Advanced

高级配置：

Max Execution Time
Max Tool Calls
Retry
Timeout
Fallback Model
Content Safety

---

22. Agent Preview

右侧始终提供：

«Live Preview»

功能：

输入消息
发送
Streaming
停止
重新生成
清空

支持查看：

Tool Call
Citation
Latency
Token Usage

---

23. Agent Debug

点击：

«Debug»

打开 Debug Console。

左侧：

Input

中间：

Execution

右侧：

Output

Execution 展开：

Prompt
 ↓
Knowledge
 ↓
LLM
 ↓
Tool
 ↓
LLM
 ↓
Output

每一步显示：

Input
Output
Duration
Tokens
Status
Error

---

24. Agent Version

顶部显示：

v1
Draft

版本操作：

Create Version
Compare
Restore
Publish
Archive

发布之后：

v1 Published

再次修改：

v2 Draft

---

25. Workflow

Workflow 是 Box 的核心可视化编排能力。

入口：

Workflows

---

26. Workflow 列表

显示：

Name
Description
Version
Status
Owner
Updated At
Actions

操作：

Open
Duplicate
Publish
Archive
Delete

---

27. Workflow Editor

布局：

┌────────────────────────────────────────────┐
│ Toolbar                                    │
├────────────┬──────────────────────┬────────┤
│ Node       │ Canvas               │ Config │
│ Library    │                      │        │
│            │                      │        │
└────────────┴──────────────────────┴────────┘

---

28. Node Library

分类：

Core

Start
Input
Output
Variable
Template

AI

LLM
Agent

Knowledge

Knowledge Search

Tool

HTTP
Function
Database
MCP

Logic

Condition
Switch
Loop
Parallel
Merge

Utility

Delay
Code

---

29. Node 配置

点击 Node 后右侧显示：

Node Name
Description
Input
Configuration
Output
Advanced

不同 Node 使用不同配置面板。

---

30. Start Node

配置：

Input Schema
Variables

---

31. LLM Node

配置：

Model
System Prompt
User Prompt
Temperature
Max Tokens
Output Format

Output：

content
usage
model

---

32. Knowledge Node

配置：

Knowledge Base
Query
Top K
Threshold
Retrieval Mode

输出：

Documents
Chunks
Citations

---

33. HTTP Node

配置：

Method
URL
Headers
Query
Body
Timeout

支持：

GET
POST
PUT
DELETE
PATCH

必须经过 SSRF 防护。

---

34. Condition Node

支持：

if
else if
else

例如：

score >= 80

---

35. Loop Node

支持：

For
For Each
While

必须限制：

Max Iterations
Timeout

防止无限循环。

---

36. Variable Node

操作：

Set
Get
Delete
Transform

---

37. Template Node

支持：

Handlebars

例如：

Hello {{user.name}}

---

38. Code Node

第一阶段可以提供受限制的代码执行能力。

必须：

Sandbox
Timeout
Memory Limit
CPU Limit
Network Restriction

禁止直接让代码访问宿主机。

---

39. Workflow Run

点击：

«Run»

系统执行：

Validate
 ↓
Create Execution
 ↓
Build Context
 ↓
Execute Nodes
 ↓
Record Trace
 ↓
Output

---

40. Workflow Debug

显示完整执行树：

Start
 │
 ├── LLM
 │
 ├── Knowledge
 │
 ├── Condition
 │    ├── True
 │    └── False
 │
 └── Output

支持点击任意节点查看：

Input
Output
Duration
Token
Status
Error

---

41. Knowledge Base

入口：

Knowledge

---

42. Knowledge Base 列表

字段：

Name
Description
Documents
Chunks
Status
Updated At

操作：

Open
Settings
Delete

---

43. 创建 Knowledge Base

字段：

Name
Description
Embedding Model
Retrieval Mode

---

44. Document

支持上传：

PDF
DOCX
TXT
Markdown
HTML

上传：

Drag & Drop
Select File

---

45. Document Processing

状态：

Uploading
Parsing
Chunking
Embedding
Indexing
Ready
Failed

显示：

Progress
Current Step
Error Message

---

46. Document Detail

显示：

File Info
Processing Status
Chunks
Metadata
Created At

可以：

Reprocess
Delete
View Chunks

---

47. Chunk Viewer

显示：

Chunk Content
Page
Position
Token Count
Metadata

支持：

Search
Pagination

---

48. RAG Test

提供：

Query

点击：

«Search»

显示：

Retrieved Chunks
Score
Document
Page
Content

然后：

«Test Answer»

查看最终 LLM 回答以及引用。

---

49. Citation

回答支持：

[1] document.pdf p.3
[2] handbook.docx p.8

点击引用可以查看原文。

---

50. Tool Management

入口：

Tools

---

51. Tool 类型

HTTP Tool
Function Tool
Database Tool
Code Tool
MCP Tool

---

52. HTTP Tool 创建

字段：

Name
Description
Method
URL
Headers
Query Parameters
Body
Input Schema
Output Schema

必须提供：

«Test Tool»

---

53. Function Tool

允许开发者定义：

Name
Description
Input Schema
Output Schema
Executor

---

54. Database Tool

支持：

MySQL

配置：

Connection
Database
SQL
Parameters

必须严格限制权限。

默认：

«禁止危险 SQL。»

---

55. MCP

MCP 页面：

MCP Servers

显示：

Name
URL
Status
Tools
Last Connected

操作：

Connect
Disconnect
Refresh
Delete

连接成功后：

Discover Tools
 ↓
Register Tools
 ↓
Agent 可以使用

---

56. Model Management

模型管理页面：

Providers
Models
Embeddings
Rerankers

---

57. Provider

配置：

Provider Name
Base URL
API Key
Status

API Key：

«加密保存。»

---

58. Model

字段：

Provider
Model Name
Type
Context Window
Enabled

类型：

Chat
Embedding
Reranker

---

59. Conversation

Conversation 页面类似现代 AI Chat 产品。

左侧：

New Chat
Conversation List

中间：

Messages

右侧：

Context
Trace

---

60. Chat

支持：

Markdown
Code
Table
Image
Attachment
Citation
Tool Call
Streaming

---

61. Message 操作

支持：

Copy
Regenerate
Retry
Delete
View Detail

---

62. Streaming

消息实时显示：

Token
 ↓
Token
 ↓
Token
 ↓
Complete

用户可以：

«Stop Generating»

---

63. Execution

执行记录页面：

Execution ID
Agent
Workflow
Status
Duration
Tokens
Created At

状态：

RUNNING
SUCCESS
FAILED
CANCELLED
TIMEOUT

---

64. Trace

Trace 页面显示：

Trace ID
Execution
Agent
Workflow

内部：

Agent
 ├── Prompt
 ├── LLM
 ├── RAG
 │    └── Search
 ├── Tool
 │    └── HTTP
 └── LLM

---

65. Analytics

Analytics 页面：

Overview

Total Requests
Success Rate
Error Rate
Tokens
Average Latency

Agent

Top Agents
Requests
Tokens
Latency
Errors

Model

Model Usage
Tokens
Latency
Errors

Workflow

Executions
Success
Failure
Average Duration

---

66. API Key

用户可以创建 API Key。

字段：

Name
Key
Created At
Last Used
Status

操作：

Create
Disable
Delete
Rotate

Key 只在创建时完整展示一次。

---

67. Agent Publish

点击：

«Publish»

弹出：

Version
Environment
Visibility

发布成功后提供：

Web Chat
API
Embed

---

68. API 发布

提供：

Endpoint
API Key
Request Example
Response Example
SDK Example

支持：

Java
JavaScript
Python
cURL

---

69. Web Chat 发布

提供：

Chat URL
Embed Code

未来可以支持：

Theme
Logo
Welcome Message
Suggested Questions

---

70. Workspace

Workspace 是资源隔离单位。

包含：

Agents
Workflows
Knowledge
Tools
Members
API Keys

---

71. Members

管理员可以：

Invite
Remove
Change Role
Disable

邀请方式：

Email

---

72. Role

支持自定义角色。

配置：

Role Name
Description
Permissions

---

73. Settings

包括：

Workspace
Members
Roles
Models
API Keys
Security

---

74. Notification

通知：

Knowledge Processing Complete
Workflow Failed
Agent Published
MCP Connection Failed
System Notification

---

75. Search

全局搜索：

Agent
Workflow
Knowledge
Conversation
Tool

支持快捷键：

Ctrl + K

---

76. Empty State

所有列表页面必须设计 Empty State。

例如：

No Agents Yet

Create your first AI Agent

按钮：

Create Agent

---

77. Loading

统一：

Skeleton
Spinner
Progress

禁止页面长时间空白。

---

78. Error State

必须提供：

Retry
View Details
Report Problem

例如：

Something went wrong.

Trace ID:
xxx

Retry

---

79. Responsive

主要目标：

«Desktop»

推荐：

1440 × 900
1920 × 1080
2560 × 1440

Workflow Editor 必须针对：

«1920 × 1080»

进行重点设计。

移动端暂时不是第一阶段目标。

---

80. Dark Mode

必须支持：

Light
Dark

所有组件：

Canvas
Editor
Chat
Table
Modal
Code
Workflow

都必须支持 Dark Mode。

---

81. 权限控制

前端：

Route Guard
Permission Guard
Button Permission

后端：

«后端权限校验必须是最终安全边界。»

前端权限不能代替后端权限。

---

82. API 错误处理

统一：

401 → Login
403 → Permission Denied
404 → Not Found
409 → Conflict
422 → Validation Error
429 → Rate Limited
500 → Server Error

---

83. Agent 删除

删除必须：

Confirm
 ↓
Check Dependencies
 ↓
Soft Delete

如果 Agent 已经发布：

«必须提示影响。»

---

84. Knowledge 删除

如果知识库被 Agent 使用：

Delete
 ↓
Check Agent Dependencies
 ↓
Warning

避免产生运行时错误。

---

85. Tool 删除

如果 Tool 正在被 Agent 使用：

«禁止直接物理删除。»

可以：

Disable

---

86. Model 删除

正在使用的模型：

«禁止直接删除。»

只能：

Disable

---

87. Workflow 删除

如果 Agent 正在绑定 Workflow：

«删除前必须提示。»

---

88. Audit Log

记录：

Login
Create Agent
Update Agent
Delete Agent
Publish Agent
Create Knowledge
Delete Knowledge
Create Tool
Execute Tool
Create API Key
Change Permission

包含：

User
IP
Action
Resource
Timestamp
Result
Trace ID

---

89. Security Center

未来提供：

API Security
Login Security
Tool Security
File Security
MCP Security
Audit

---

90. Agent Template

未来提供模板：

Customer Service Agent
RAG Knowledge Agent
Data Analyst
Marketing Assistant
Coding Assistant
Document Assistant

用户可以：

Use Template
 ↓
Create Agent

---

91. Marketplace

暂不作为 V1 核心功能。

未来支持：

Agent Marketplace
Workflow Marketplace
Tool Marketplace
Prompt Marketplace
MCP Marketplace

---

92. 产品核心闭环

Box 最终必须实现：

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
                      ↓
                    Version
                      ↓
                    Publish

---

93. 核心业务流程

创建 Agent

User
 ↓
Create Agent
 ↓
Agent Draft
 ↓
Configure
 ↓
Save

---

Agent 执行

User
 ↓
API / Web Chat
 ↓
Agent Runtime
 ↓
Load Version
 ↓
Build Context
 ↓
Prompt
 ↓
Memory
 ↓
RAG
 ↓
Tool
 ↓
LangChain4j
 ↓
LLM
 ↓
Response
 ↓
Trace
 ↓
Conversation

---

94. Workflow 执行

Request
 ↓
Workflow Runtime
 ↓
Start
 ↓
Node
 ↓
Context
 ↓
Next Node
 ↓
...
 ↓
Output

---

95. Knowledge RAG

Upload
 ↓
MinIO
 ↓
Parser
 ↓
Cleaner
 ↓
Chunk
 ↓
Embedding
 ↓
Elasticsearch
 ↓
Ready

查询：

Question
 ↓
Embedding
 ↓
Hybrid Search
 ↓
Top K
 ↓
Rerank
 ↓
Context
 ↓
LLM
 ↓
Citation

---

96. Tool Calling

User
 ↓
Agent
 ↓
LLM
 ↓
Tool Call
 ↓
Permission
 ↓
Security
 ↓
Tool Executor
 ↓
Tool Result
 ↓
LLM
 ↓
Answer

---

97. MVP 范围

第一版必须优先完成：

Authentication
Workspace
Agent
Model
Conversation
Agent Runtime
Workflow
Workflow Runtime
Knowledge
RAG
Tool
Publish
API Key
Trace
RBAC

暂时不追求：

Marketplace
Billing
复杂团队协作
微服务
Kafka
Kubernetes
复杂插件市场

---

98. MVP 用户体验

用户第一次进入系统后：

Dashboard
 ↓
Create Agent
 ↓
填写名称
 ↓
选择模型
 ↓
填写 Prompt
 ↓
测试
 ↓
绑定 Knowledge
 ↓
绑定 Tool
 ↓
保存
 ↓
Publish
 ↓
获得 API

整个流程应该尽量：

«低学习成本、低配置成本、低认知负担。»

---

99. 产品设计原则

必须遵循：

简洁

不要让用户面对大量复杂配置。

渐进式披露

简单配置默认展示。

高级配置折叠。

可视化

复杂 AI 能力尽量可视化。

可调试

任何 Agent 行为都应该能够解释：

为什么这样回答？
用了什么知识？
调用了什么 Tool？
用了多少 Token？
耗时多少？

可观测

用户必须可以查看：

Execution
Trace
Token
Latency
Error

---

100. 最终产品定位

Box 不是：

«一个简单的 AI Chat 网站。»

也不是：

«一个简单的 Prompt 管理工具。»

而是：

AI Agent Platform

核心产品能力：

┌────────────────────────────────────────┐
│              Box Platform            │
├────────────────────────────────────────┤
│                                        │
│  Agent                                  │
│    ├── Prompt                           │
│    ├── Model                            │
│    ├── Knowledge                        │
│    ├── Tools                            │
│    ├── Memory                           │
│    └── Workflow                         │
│                                        │
│  Runtime                                │
│    ├── Agent Runtime                    │
│    ├── Workflow Runtime                 │
│    └── AI Orchestrator                  │
│                                        │
│  AI                                     │
│    ├── LLM                              │
│    ├── RAG                              │
│    ├── Tool                             │
│    └── MCP                              │
│                                        │
│  Platform                               │
│    ├── Multi-Tenant                     │
│    ├── RBAC                             │
│    ├── Publish                          │
│    ├── API                              │
│    ├── Trace                            │
│    └── Analytics                        │
│                                        │
└────────────────────────────────────────┘

最终目标：

«让不会编写复杂 AI Agent 代码的用户，也能够通过 Box 创建、编排、调试并发布真正可运行的 AI Agent。»