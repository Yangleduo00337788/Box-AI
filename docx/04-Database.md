Box Database

企业级 AI Agent / Workflow 平台数据库设计

项目： Box
数据库： MySQL 8.0+
字符集： utf8mb4
存储引擎： InnoDB
主键： BIGINT
时间： DATETIME(3)
软删除： deleted
多租户隔离： workspace_id
配置型数据： JSON
向量数据： Elasticsearch
文件： MinIO

---

一、数据库设计总览

Box 数据库按照以下领域划分：

01 用户与权限
02 Workspace
03 Agent
04 Model
05 Knowledge
06 Tool
07 Workflow
08 Conversation
09 Runtime
10 Trace
11 Publish
12 System / Audit

最终核心表：

用户权限
├── sys_user
├── sys_role
├── sys_permission
├── sys_user_role
└── sys_role_permission

Workspace
├── workspace
└── workspace_member

Agent
├── agent
├── agent_version
├── agent_variable
├── agent_knowledge
└── agent_tool

Model
├── model_provider
├── model
├── model_api_key
└── model_usage

Knowledge
├── knowledge_base
├── knowledge_document
└── knowledge_chunk

Tool
├── tool
├── tool_http_config
├── tool_database_config
├── tool_function_config
└── tool_mcp_config

Workflow
├── workflow
├── workflow_version
├── workflow_node
└── workflow_edge

Conversation
├── conversation
├── message
└── message_attachment

Runtime
├── execution
└── execution_node

Trace
├── trace
└── trace_span

Publish
├── publish
├── api_key
└── api_key_permission

System
├── audit_log
└── system_config

---

二、统一数据库规范

所有业务表建议遵循：

id
workspace_id
created_by
created_at
updated_by
updated_at
deleted

基础字段：

id BIGINT NOT NULL PRIMARY KEY,
created_at DATETIME(3) NOT NULL,
updated_at DATETIME(3) NOT NULL,
deleted TINYINT NOT NULL DEFAULT 0

对于 Workspace 级资源：

workspace_id BIGINT NOT NULL

这样可以保证：

Workspace A
    ↓
只能看到 A 的 Agent

Workspace B
    ↓
只能看到 B 的 Agent

---

三、ID 设计

不建议使用：

INT

统一：

BIGINT UNSIGNED

例如：

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT

Java：

Long id;

---

四、用户表

4.1 sys_user

用户基础信息。

CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',

    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',

    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',

    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(512) DEFAULT NULL COMMENT '头像',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',

    last_login_at DATETIME(3) DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='系统用户';

---

五、角色表

5.1 sys_role

CREATE TABLE sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    code VARCHAR(64) NOT NULL COMMENT '角色编码',
    description VARCHAR(255) DEFAULT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='系统角色';

---

六、权限表

6.1 sys_permission

CREATE TABLE sys_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    name VARCHAR(128) NOT NULL,
    code VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL COMMENT 'MENU/BUTTON/API',
    description VARCHAR(255) DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='系统权限';

例如：

agent:create
agent:read
agent:update
agent:delete
agent:publish

workflow:create
workflow:update
workflow:execute

knowledge:create
knowledge:delete

tool:create
tool:execute

---

七、用户角色关系

7.1 sys_user_role

CREATE TABLE sys_user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_user_role (
        user_id,
        role_id
    ),

    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='用户角色关系';

---

八、角色权限关系

8.1 sys_role_permission

CREATE TABLE sys_role_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_role_permission (
        role_id,
        permission_id
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='角色权限关系';

---

九、Workspace

Workspace 是 Box 最重要的数据隔离边界。

9.1 workspace

CREATE TABLE workspace (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,

    description VARCHAR(512) DEFAULT NULL,
    avatar VARCHAR(512) DEFAULT NULL,

    owner_id BIGINT UNSIGNED NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_code (code),

    KEY idx_owner_id (owner_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='工作空间';

---

十、Workspace Member

10.1 workspace_member

CREATE TABLE workspace_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,

    role_id BIGINT UNSIGNED NOT NULL,

    nickname VARCHAR(64) DEFAULT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    joined_at DATETIME(3) NOT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_workspace_user (
        workspace_id,
        user_id
    ),

    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='工作空间成员';

---

十一、Agent

11.1 agent

Agent 本身只保存基础信息。

CREATE TABLE agent (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    avatar VARCHAR(512) DEFAULT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    draft_version_id BIGINT UNSIGNED DEFAULT NULL,
    published_version_id BIGINT UNSIGNED DEFAULT NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_by BIGINT UNSIGNED DEFAULT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_status (status),
    KEY idx_created_by (created_by)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='AI Agent';

---

十二、Agent Version

这是 Agent 最核心的表。

12.1 agent_version

CREATE TABLE agent_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,

    version_no INT NOT NULL,

    name VARCHAR(128) DEFAULT NULL,

    system_prompt LONGTEXT,

    model_config JSON DEFAULT NULL,

    knowledge_config JSON DEFAULT NULL,

    tool_config JSON DEFAULT NULL,

    memory_config JSON DEFAULT NULL,

    workflow_config JSON DEFAULT NULL,

    variable_config JSON DEFAULT NULL,

    advanced_config JSON DEFAULT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    change_log VARCHAR(512) DEFAULT NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_agent_version (
        agent_id,
        version_no
    ),

    KEY idx_workspace_id (workspace_id),
    KEY idx_agent_id (agent_id),
    KEY idx_status (status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Agent版本';

---

十三、为什么 Agent Version 使用 JSON

例如：

{
  "temperature": 0.7,
  "topP": 0.9,
  "maxTokens": 4096,
  "stream": true
}

模型配置变化非常频繁。

如果全部拆成：

agent_model
agent_model_parameter
agent_prompt_config
agent_memory_config
...

开发复杂度会快速上升。

V1：

稳定关系 → 表
变化频繁配置 → JSON

这是比较合理的折中。

---

十四、Agent Variable

14.1 agent_variable

CREATE TABLE agent_variable (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED DEFAULT NULL,

    name VARCHAR(128) NOT NULL,
    variable_key VARCHAR(128) NOT NULL,

    data_type VARCHAR(32) NOT NULL,

    default_value TEXT DEFAULT NULL,

    required TINYINT NOT NULL DEFAULT 0,

    description VARCHAR(512) DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_agent_id (agent_id),
    KEY idx_version_id (version_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Agent变量';

---

十五、Agent Knowledge

15.1 agent_knowledge

CREATE TABLE agent_knowledge (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,

    knowledge_base_id BIGINT UNSIGNED NOT NULL,

    top_k INT NOT NULL DEFAULT 5,

    score_threshold DECIMAL(8,6) DEFAULT NULL,

    retrieval_mode VARCHAR(32) NOT NULL DEFAULT 'HYBRID',

    rerank_enabled TINYINT NOT NULL DEFAULT 1,

    citation_enabled TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_version_knowledge (
        version_id,
        knowledge_base_id
    ),

    KEY idx_agent_id (agent_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Agent知识库关系';

---

十六、Agent Tool

16.1 agent_tool

CREATE TABLE agent_tool (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,

    tool_id BIGINT UNSIGNED NOT NULL,

    enabled TINYINT NOT NULL DEFAULT 1,

    require_confirmation TINYINT NOT NULL DEFAULT 0,

    config JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_version_tool (
        version_id,
        tool_id
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Agent工具关系';

---

十七、Model Provider

17.1 model_provider

CREATE TABLE model_provider (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED DEFAULT NULL,

    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,

    base_url VARCHAR(512) DEFAULT NULL,

    provider_type VARCHAR(32) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    UNIQUE KEY uk_workspace_code (
        workspace_id,
        code
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='模型提供商';

---

十八、Model

18.1 model

CREATE TABLE model (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    provider_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED DEFAULT NULL,

    name VARCHAR(128) NOT NULL,
    model_code VARCHAR(128) NOT NULL,

    model_type VARCHAR(32) NOT NULL
        COMMENT 'CHAT/EMBEDDING/RERANK',

    context_window INT DEFAULT NULL,

    capabilities JSON DEFAULT NULL,

    config JSON DEFAULT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_provider_id (provider_id),
    KEY idx_workspace_id (workspace_id),
    KEY idx_model_type (model_type)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='AI模型';

---

十九、Model API Key

19.1 model_api_key

API Key 必须加密。

CREATE TABLE model_api_key (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,
    provider_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,

    api_key_ciphertext TEXT NOT NULL,

    api_key_masked VARCHAR(128) DEFAULT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_by BIGINT UNSIGNED NOT NULL,

    last_used_at DATETIME(3) DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_provider_id (provider_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='模型API Key';

绝对不要：

api_key VARCHAR(...)

明文存储。

---

二十、Knowledge Base

20.1 knowledge_base

CREATE TABLE knowledge_base (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    icon VARCHAR(512) DEFAULT NULL,

    embedding_model_id BIGINT UNSIGNED DEFAULT NULL,
    rerank_model_id BIGINT UNSIGNED DEFAULT NULL,

    chunk_config JSON DEFAULT NULL,

    retrieval_config JSON DEFAULT NULL,

    document_count INT NOT NULL DEFAULT 0,
    chunk_count BIGINT NOT NULL DEFAULT 0,

    status VARCHAR(32) NOT NULL DEFAULT 'READY',

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_status (status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='知识库';

---

二十一、Knowledge Document

21.1 knowledge_document

CREATE TABLE knowledge_document (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,
    knowledge_base_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,

    file_type VARCHAR(32) NOT NULL,
    mime_type VARCHAR(128) DEFAULT NULL,

    file_size BIGINT DEFAULT NULL,

    storage_bucket VARCHAR(128) DEFAULT NULL,
    storage_key VARCHAR(1024) DEFAULT NULL,

    md5 VARCHAR(64) DEFAULT NULL,

    page_count INT DEFAULT NULL,
    chunk_count INT NOT NULL DEFAULT 0,

    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADING',

    error_message TEXT DEFAULT NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_knowledge_base_id (knowledge_base_id),
    KEY idx_status (status),
    KEY idx_md5 (md5)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='知识库文档';

状态：

UPLOADING
PARSING
CHUNKING
EMBEDDING
INDEXING
READY
FAILED

---

二十二、Knowledge Chunk

22.1 knowledge_chunk

注意：

«Chunk 的向量不放 MySQL。»

MySQL 只保存元数据。

CREATE TABLE knowledge_chunk (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,
    knowledge_base_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,

    chunk_index INT NOT NULL,

    content LONGTEXT NOT NULL,

    token_count INT DEFAULT NULL,

    page_number INT DEFAULT NULL,

    metadata JSON DEFAULT NULL,

    es_document_id VARCHAR(128) NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'INDEXED',

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_knowledge_base_id (knowledge_base_id),
    KEY idx_document_id (document_id),

    UNIQUE KEY uk_es_document_id (
        es_document_id
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='知识库文本Chunk';

---

二十三、Elasticsearch Chunk

ES Index：

box_knowledge_chunk

核心结构：

{
  "id": "123",
  "workspace_id": "10001",
  "knowledge_base_id": "20001",
  "document_id": "30001",
  "chunk_id": "40001",

  "content": "Box 是一个 AI Agent 平台...",

  "vector": [0.0123, -0.0234],

  "metadata": {
    "page": 3,
    "file_name": "example.pdf"
  }
}

其中：

content
vector
metadata

主要用于检索。

---

二十四、Tool

24.1 tool

统一 Tool 主表：

CREATE TABLE tool (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    tool_key VARCHAR(128) NOT NULL,

    description VARCHAR(512) DEFAULT NULL,

    type VARCHAR(32) NOT NULL
        COMMENT 'HTTP/FUNCTION/DATABASE/CODE/MCP',

    input_schema JSON DEFAULT NULL,
    output_schema JSON DEFAULT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_workspace_tool_key (
        workspace_id,
        tool_key
    ),

    KEY idx_workspace_id (workspace_id),
    KEY idx_type (type)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Agent Tool';

---

二十五、HTTP Tool

25.1 tool_http_config

CREATE TABLE tool_http_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    tool_id BIGINT UNSIGNED NOT NULL,

    method VARCHAR(16) NOT NULL,
    url VARCHAR(2048) NOT NULL,

    headers JSON DEFAULT NULL,
    query_params JSON DEFAULT NULL,

    body_type VARCHAR(32) DEFAULT NULL,
    body_template LONGTEXT DEFAULT NULL,

    timeout_ms INT NOT NULL DEFAULT 10000,

    allow_redirect TINYINT NOT NULL DEFAULT 0,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_tool_id (tool_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='HTTP Tool配置';

---

二十六、Database Tool

26.1 tool_database_config

CREATE TABLE tool_database_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    tool_id BIGINT UNSIGNED NOT NULL,

    database_type VARCHAR(32) NOT NULL DEFAULT 'MYSQL',

    host VARCHAR(255) NOT NULL,
    port INT NOT NULL DEFAULT 3306,

    database_name VARCHAR(128) NOT NULL,

    username VARCHAR(128) NOT NULL,
    password_ciphertext TEXT NOT NULL,

    allowed_operations JSON DEFAULT NULL,

    max_rows INT NOT NULL DEFAULT 100,

    timeout_ms INT NOT NULL DEFAULT 10000,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_tool_id (tool_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='数据库Tool配置';

---

二十七、Function Tool

27.1 tool_function_config

CREATE TABLE tool_function_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    tool_id BIGINT UNSIGNED NOT NULL,

    function_name VARCHAR(128) NOT NULL,

    function_code LONGTEXT NOT NULL,

    runtime VARCHAR(32) NOT NULL DEFAULT 'JAVA_SCRIPT',

    timeout_ms INT NOT NULL DEFAULT 5000,

    memory_limit_mb INT NOT NULL DEFAULT 128,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_tool_id (tool_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Function Tool配置';

---

二十八、MCP Tool

28.1 tool_mcp_config

CREATE TABLE tool_mcp_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    tool_id BIGINT UNSIGNED NOT NULL,

    server_name VARCHAR(128) NOT NULL,
    server_url VARCHAR(1024) NOT NULL,

    tool_name VARCHAR(128) NOT NULL,

    auth_config JSON DEFAULT NULL,

    input_schema JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_tool_mcp (
        tool_id
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='MCP Tool配置';

---

二十九、Workflow

29.1 workflow

CREATE TABLE workflow (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    draft_version_id BIGINT UNSIGNED DEFAULT NULL,
    published_version_id BIGINT UNSIGNED DEFAULT NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_status (status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Workflow';

---

三十、Workflow Version

30.1 workflow_version

CREATE TABLE workflow_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workflow_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,

    version_no INT NOT NULL,

    definition JSON NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    change_log VARCHAR(512) DEFAULT NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_workflow_version (
        workflow_id,
        version_no
    ),

    KEY idx_workspace_id (workspace_id),
    KEY idx_workflow_id (workflow_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Workflow版本';

---

三十一、为什么 Workflow Version 使用 definition JSON

Workflow 本质上是一张图。

例如：

Node
Edge
Node
Edge
Condition
Config
Position

如果强制完全关系型保存，会导致大量复杂 JOIN。

因此 V1：

workflow
workflow_version
      │
      └── definition JSON

同时可以根据运行和分析需要，将 Node/Edge 拆成独立表。

推荐：

«数据库保存结构化 Node/Edge + Version 保存完整 definition 快照。»

这样兼顾可维护性和版本回滚。

---

三十二、Workflow Node

CREATE TABLE workflow_node (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workflow_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,

    node_key VARCHAR(128) NOT NULL,

    node_type VARCHAR(64) NOT NULL,

    name VARCHAR(128) NOT NULL,

    position_x DECIMAL(12,4) NOT NULL DEFAULT 0,
    position_y DECIMAL(12,4) NOT NULL DEFAULT 0,

    config JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_version_node (
        version_id,
        node_key
    ),

    KEY idx_workflow_id (workflow_id),
    KEY idx_version_id (version_id),
    KEY idx_node_type (node_type)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Workflow节点';

---

三十三、Workflow Edge

CREATE TABLE workflow_edge (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workflow_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,

    source_node_key VARCHAR(128) NOT NULL,
    target_node_key VARCHAR(128) NOT NULL,

    source_port VARCHAR(64) DEFAULT NULL,
    target_port VARCHAR(64) DEFAULT NULL,

    condition_expression TEXT DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_version_id (version_id),
    KEY idx_source_node (source_node_key),
    KEY idx_target_node (target_node_key)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Workflow边';

---

三十四、Conversation

34.1 conversation

CREATE TABLE conversation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    agent_id BIGINT UNSIGNED NOT NULL,
    agent_version_id BIGINT UNSIGNED DEFAULT NULL,

    user_id BIGINT UNSIGNED DEFAULT NULL,

    title VARCHAR(255) DEFAULT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',

    message_count INT NOT NULL DEFAULT 0,

    last_message_at DATETIME(3) DEFAULT NULL,

    metadata JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_agent_id (agent_id),
    KEY idx_user_id (user_id),
    KEY idx_last_message_at (last_message_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='AI会话';

---

三十五、Message

35.1 message

CREATE TABLE message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    conversation_id BIGINT UNSIGNED NOT NULL,

    workspace_id BIGINT UNSIGNED NOT NULL,

    role VARCHAR(32) NOT NULL
        COMMENT 'SYSTEM/USER/ASSISTANT/TOOL',

    content LONGTEXT,

    content_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',

    sequence_no INT NOT NULL,

    token_count INT DEFAULT NULL,

    model_id BIGINT UNSIGNED DEFAULT NULL,

    metadata JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_conversation_sequence (
        conversation_id,
        sequence_no
    ),

    KEY idx_conversation_id (conversation_id),
    KEY idx_workspace_id (workspace_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='消息';

---

三十六、Message Attachment

CREATE TABLE message_attachment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    message_id BIGINT UNSIGNED NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64) DEFAULT NULL,
    mime_type VARCHAR(128) DEFAULT NULL,
    file_size BIGINT DEFAULT NULL,

    storage_bucket VARCHAR(128) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_message_id (message_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='消息附件';

---

三十七、Execution

37.1 execution

这是整个 Runtime 的核心表。

CREATE TABLE execution (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    execution_no VARCHAR(64) NOT NULL,

    workspace_id BIGINT UNSIGNED NOT NULL,

    execution_type VARCHAR(32) NOT NULL
        COMMENT 'AGENT/WORKFLOW',

    agent_id BIGINT UNSIGNED DEFAULT NULL,
    agent_version_id BIGINT UNSIGNED DEFAULT NULL,

    workflow_id BIGINT UNSIGNED DEFAULT NULL,
    workflow_version_id BIGINT UNSIGNED DEFAULT NULL,

    conversation_id BIGINT UNSIGNED DEFAULT NULL,

    user_id BIGINT UNSIGNED DEFAULT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',

    input JSON DEFAULT NULL,
    output JSON DEFAULT NULL,

    error_code VARCHAR(128) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,

    started_at DATETIME(3) DEFAULT NULL,
    finished_at DATETIME(3) DEFAULT NULL,

    duration_ms BIGINT DEFAULT NULL,

    total_tokens INT DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_execution_no (
        execution_no
    ),

    KEY idx_workspace_id (workspace_id),
    KEY idx_agent_id (agent_id),
    KEY idx_workflow_id (workflow_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Agent/Workflow执行记录';

---

三十八、Execution Node

CREATE TABLE execution_node (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    execution_id BIGINT UNSIGNED NOT NULL,

    node_key VARCHAR(128) DEFAULT NULL,
    node_type VARCHAR(64) DEFAULT NULL,

    status VARCHAR(32) NOT NULL,

    input JSON DEFAULT NULL,
    output JSON DEFAULT NULL,

    error_code VARCHAR(128) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,

    started_at DATETIME(3) DEFAULT NULL,
    finished_at DATETIME(3) DEFAULT NULL,

    duration_ms BIGINT DEFAULT NULL,

    input_tokens INT DEFAULT NULL,
    output_tokens INT DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_execution_id (execution_id),
    KEY idx_node_type (node_type),
    KEY idx_status (status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Workflow节点执行记录';

---

三十九、Trace

CREATE TABLE trace (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    trace_id VARCHAR(64) NOT NULL,

    execution_id BIGINT UNSIGNED NOT NULL,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,

    status VARCHAR(32) NOT NULL,

    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) DEFAULT NULL,

    duration_ms BIGINT DEFAULT NULL,

    metadata JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_trace_id (
        trace_id
    ),

    KEY idx_execution_id (execution_id),
    KEY idx_workspace_id (workspace_id),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='执行Trace';

---

四十、Trace Span

CREATE TABLE trace_span (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    trace_id VARCHAR(64) NOT NULL,

    span_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64) DEFAULT NULL,

    span_type VARCHAR(32) NOT NULL
        COMMENT 'LLM/RAG/TOOL/NODE/MEMORY',

    name VARCHAR(128) NOT NULL,

    status VARCHAR(32) NOT NULL,

    input JSON DEFAULT NULL,
    output JSON DEFAULT NULL,

    model_id BIGINT UNSIGNED DEFAULT NULL,
    tool_id BIGINT UNSIGNED DEFAULT NULL,

    input_tokens INT DEFAULT NULL,
    output_tokens INT DEFAULT NULL,

    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) DEFAULT NULL,

    duration_ms BIGINT DEFAULT NULL,

    error_code VARCHAR(128) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,

    metadata JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_span_id (
        span_id
    ),

    KEY idx_trace_id (trace_id),
    KEY idx_parent_span_id (parent_span_id),
    KEY idx_span_type (span_type),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Trace Span';

---

四十一、Publish

41.1 publish

CREATE TABLE publish (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    resource_type VARCHAR(32) NOT NULL
        COMMENT 'AGENT/WORKFLOW',

    resource_id BIGINT UNSIGNED NOT NULL,

    version_id BIGINT UNSIGNED NOT NULL,

    channel VARCHAR(32) NOT NULL
        COMMENT 'WEB/API/EMBED',

    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',

    published_by BIGINT UNSIGNED NOT NULL,

    published_at DATETIME(3) NOT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_resource (resource_type, resource_id),
    KEY idx_version_id (version_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='发布记录';

---

四十二、API Key

42.1 api_key

CREATE TABLE api_key (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,

    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(128) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    expires_at DATETIME(3) DEFAULT NULL,
    last_used_at DATETIME(3) DEFAULT NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_key_hash (
        key_hash
    ),

    KEY idx_workspace_id (workspace_id),
    KEY idx_status (status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='开放API Key';

---

四十三、API Key Permission

CREATE TABLE api_key_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    api_key_id BIGINT UNSIGNED NOT NULL,

    resource_type VARCHAR(32) NOT NULL,
    resource_id BIGINT UNSIGNED DEFAULT NULL,

    permission VARCHAR(64) NOT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_api_key_id (api_key_id),

    UNIQUE KEY uk_api_permission (
        api_key_id,
        resource_type,
        resource_id,
        permission
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='API Key权限';

---

四十四、Model Usage

用于统计 Token。

CREATE TABLE model_usage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    execution_id BIGINT UNSIGNED DEFAULT NULL,

    model_id BIGINT UNSIGNED NOT NULL,

    provider_id BIGINT UNSIGNED NOT NULL,

    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,

    request_count INT NOT NULL DEFAULT 1,

    latency_ms BIGINT DEFAULT NULL,

    estimated_cost DECIMAL(18,8) DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_model_id (model_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='模型调用统计';

---

四十五、Audit Log

CREATE TABLE audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED DEFAULT NULL,

    user_id BIGINT UNSIGNED DEFAULT NULL,

    action VARCHAR(128) NOT NULL,

    resource_type VARCHAR(64) DEFAULT NULL,
    resource_id BIGINT UNSIGNED DEFAULT NULL,

    request_id VARCHAR(64) DEFAULT NULL,

    ip VARCHAR(64) DEFAULT NULL,

    user_agent VARCHAR(512) DEFAULT NULL,

    detail JSON DEFAULT NULL,

    created_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_workspace_id (workspace_id),
    KEY idx_user_id (user_id),
    KEY idx_resource (
        resource_type,
        resource_id
    ),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='审计日志';

---

四十六、System Config

CREATE TABLE system_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,

    description VARCHAR(512) DEFAULT NULL,

    encrypted TINYINT NOT NULL DEFAULT 0,

    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_config_key (
        config_key
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='系统配置';

---

四十七、核心 ER 关系

整个 Box 最重要的关系：

                    sys_user
                       │
                       │
                       ▼
                workspace_member
                       │
                       ▼
                  workspace
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
      Agent         Workflow      KnowledgeBase
        │              │              │
        ▼              ▼              ▼
  AgentVersion   WorkflowVersion   Document
        │              │              │
   ┌────┼────┐         │              ▼
   │    │    │         │            Chunk
   ▼    ▼    ▼         │
Model Knowledge Tool    │
                        ▼
                      Node
                        │
                        ▼
                       Edge

---

四十八、Agent 完整关系

Workspace
   │
   ▼
 Agent
   │
   ▼
AgentVersion
   │
   ├──────────────┐
   ▼              ▼
Model         Knowledge
   │              │
   │              ▼
   │         KnowledgeBase
   │              │
   │              ▼
   │          Document
   │              │
   │              ▼
   │            Chunk
   │
   └──────────────┐
                  ▼
                Tool

---

四十九、Agent Version 为什么必须存在

假设用户现在：

生产环境：
Agent Version 5

然后开发人员修改：

Prompt
Model
Tool
Knowledge

此时不能直接覆盖生产配置。

应该：

Version 5
    ↓
Published

Version 6
    ↓
Draft

测试完成：

Version 6
    ↓
Publish

于是：

Production
    ↓
Version 6

这样可以实现：

版本管理
版本回滚
版本对比
灰度发布
审计

---

五十、Workflow Version

同样：

Workflow Version 1
Workflow Version 2
Workflow Version 3

发布：

Published Version

Runtime 永远：

读取 Published Version

而不是：

读取 Draft

---

五十一、Workspace 数据隔离

这是后端必须强制执行的。

例如：

GET /api/v1/agents/10001

不能只：

SELECT *
FROM agent
WHERE id = 10001;

必须：

SELECT *
FROM agent
WHERE id = 10001
  AND workspace_id = #{workspaceId}
  AND deleted = 0;

即：

Resource ID
+
Workspace ID

双重验证。

---

五十二、为什么所有业务表都推荐 workspace_id

例如：

agent
workflow
knowledge_base
tool
conversation
execution
trace
api_key

全部包含：

workspace_id

这样可以：

快速过滤
权限验证
数据隔离
统计分析
未来多租户

---

五十三、Soft Delete

业务资源：

Agent
Workflow
Knowledge
Tool
Conversation
API Key

使用：

deleted TINYINT DEFAULT 0

删除：

UPDATE agent
SET deleted = 1
WHERE id = ?
AND workspace_id = ?;

不要直接：

DELETE FROM agent;

---

五十四、外键策略

V1 不建议大量使用 MySQL Foreign Key。

例如：

agent.agent_id
agent_version.agent_id
workflow_node.workflow_id

逻辑关系由应用层保证。

原因：

复杂删除
版本管理
批量操作
未来拆服务

都会因为数据库外键产生额外约束。

因此：

«关系存在，但不强制依赖物理 FK。»

---

五十五、索引原则

不要：

所有字段都加索引

重点索引：

workspace_id
status
created_at
updated_at
resource_id
user_id
agent_id
workflow_id

高频查询：

WHERE workspace_id = ?
AND deleted = 0

建议联合索引：

KEY idx_workspace_deleted (
    workspace_id,
    deleted
)

在实际建表时可以进一步根据 SQL 查询计划优化。

---

五十六、JSON 使用原则

适合 JSON：

model_config
knowledge_config
tool_config
memory_config
workflow_config
metadata
node_config
capabilities

不适合 JSON：

user
agent
workflow
knowledge_base
document
conversation
message

原则：

«业务实体关系结构化，动态配置 JSON 化。»

---

五十七、数据存储职责

最终：

                    Box
                      │
       ┌──────────────┼──────────────┐
       ▼              ▼              ▼
     MySQL          Redis          MinIO
       │              │              │
       │              │              └── 文件
       │              │
       │              └── Cache
       │                  Session
       │                  Rate Limit
       │
       └── 业务数据


                      │
                      ▼
               Elasticsearch
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Keyword      Vector      Hybrid

---

五十八、一次完整聊天的数据流

用户发送：

你好，请介绍一下 Box

流程：

User
 ↓
API
 ↓
API Key
 ↓
Workspace
 ↓
Agent
 ↓
Published AgentVersion
 ↓
Conversation
 ↓
Message
 ↓
AgentRuntime
 ↓
Prompt
 ↓
Memory
 ↓
Knowledge
 ↓
Tool
 ↓
LangChain4j
 ↓
LLM
 ↓
SSE
 ↓
Assistant Message

同时：

Execution
Trace
TraceSpan
ModelUsage

全部产生记录。

---

五十九、一次 Workflow 执行

POST /workflows/{id}/execute

执行：

Workflow
 ↓
Published Version
 ↓
Execution
 ↓
Start Node
 ↓
Input Node
 ↓
Knowledge Node
 ↓
LLM Node
 ↓
Condition Node
 ├── TRUE → HTTP Node
 │
 └── FALSE → LLM Node
                ↓
              Output

数据库：

execution
    │
    ├── execution_node
    │
    └── trace
            │
            └── trace_span

---

六十、数据库目录建议

后端：

box-server
└── box-infrastructure
    └── src/main/resources
        └── db
            ├── migration
            │   ├── V1__init.sql
            │   ├── V2__rbac.sql
            │   ├── V3__agent.sql
            │   ├── V4__model.sql
            │   ├── V5__knowledge.sql
            │   ├── V6__tool.sql
            │   ├── V7__workflow.sql
            │   ├── V8__conversation.sql
            │   ├── V9__runtime.sql
            │   └── V10__publish.sql
            │
            └── seed
                ├── roles.sql
                ├── permissions.sql
                └── models.sql

推荐使用：

Flyway

管理数据库版本。

---

六十一、数据库初始化顺序

不要一次性手写一个巨大 SQL。

推荐：

V1
 ↓
User
 ↓
RBAC
 ↓
Workspace
 ↓
Agent
 ↓
Model
 ↓
Knowledge
 ↓
Tool
 ↓
Workflow
 ↓
Conversation
 ↓
Runtime
 ↓
Trace
 ↓
Publish

这样以后升级非常清晰。

---

六十二、V1 数据库最终规模

核心业务表约：

用户权限              5
Workspace             2
Agent                 5
Model                 3
Knowledge             3
Tool                  5
Workflow              4
Conversation          3
Runtime               2
Trace                 2
Publish               2
System                2
──────────────────────
合计                 ≈38

再根据实际业务增加：

Memory
MCP Server
Plugin
Webhook
Invitation
Notification
Usage Daily
Quota

最终 V1.5 大约：

40～50 张表

---

六十三、最重要的数据库架构决策

Box 数据库最终遵循：

┌─────────────────────────────────────┐
│              MySQL                  │
│                                     │
│ User                                │
│ Workspace                           │
│ Agent                               │
│ Agent Version                       │
│ Workflow                            │
│ Workflow Version                    │
│ Knowledge Metadata                  │
│ Tool                                │
│ Model                               │
│ Conversation                        │
│ Execution                           │
│ Trace                               │
│ Publish                             │
└─────────────────────────────────────┘

                │
                │

┌─────────────────────────────────────┐
│         Elasticsearch               │
│                                     │
│ Full Text Search                    │
│ Vector Search                       │
│ Hybrid Search                       │
│ Knowledge Chunk                     │
└─────────────────────────────────────┘

                │

┌─────────────────────────────────────┐
│              MinIO                  │
│                                     │
│ PDF                                 │
│ DOCX                                │
│ Images                              │
│ Attachments                         │
└─────────────────────────────────────┘

                │

┌─────────────────────────────────────┐
│               Redis                 │
│                                     │
│ Cache                               │
│ Session                             │
│ Rate Limit                          │
│ Runtime Temporary State             │
└─────────────────────────────────────┘

---

六十四、最终核心关系图

                         USER
                          │
                          ▼
                   WORKSPACE MEMBER
                          │
                          ▼
                      WORKSPACE
                          │
       ┌──────────────────┼──────────────────┐
       │                  │                  │
       ▼                  ▼                  ▼
     AGENT             WORKFLOW          KNOWLEDGE
       │                  │                  │
       ▼                  ▼                  ▼
AGENT_VERSION      WORKFLOW_VERSION       DOCUMENT
       │                  │                  │
   ┌───┼────┐             │                  ▼
   │   │    │             ▼                CHUNK
   │   │    │           NODE
   │   │    │             │
   │   │    │             ▼
   │   │    │            EDGE
   │   │    │
   │   │    └────────── TOOL
   │   │
   │   └────────────── KNOWLEDGE
   │
   └────────────────── MODEL


AGENT / WORKFLOW
       │
       ▼
   EXECUTION
       │
       ├─────────────── EXECUTION_NODE
       │
       ▼
     TRACE
       │
       ▼
   TRACE_SPAN
       │
       ├── LLM
       ├── RAG
       ├── TOOL
       ├── NODE
       └── MEMORY


AGENT
  │
  ▼
PUBLISH
  │
  ▼
API_KEY

六十五、第四部分结论

Box V1 数据层最终采用：

MySQL
    ↓
核心业务数据

Elasticsearch
    ↓
RAG / Vector / Search

MinIO
    ↓
文件

Redis
    ↓
缓存 / Session / 限流 / 临时运行状态

最核心的三个设计：

① Workspace 隔离
② Agent / Workflow Version
③ Runtime / Trace 独立

其中：

Agent
    ≠
Agent Runtime

Workflow
    ≠
Workflow Runtime

Draft
    ≠
Published

MySQL
    ≠
Vector Database

MinIO
    ≠
Business Database

这几个边界确定以后，后面的代码结构会非常稳定。