Box 第七部分：MySQL 数据库设计

一、数据库总体定位

Box 使用：

MySQL
├── 用户
├── 工作空间
├── RBAC
├── Agent
├── Agent Version
├── Model
├── Knowledge
├── Tool
├── Workflow
├── Conversation
├── Runtime
├── Publish
├── API Key
├── Trace
└── Analytics

其他基础设施：

MySQL
    │
    ├── 业务数据
    │
    └── 元数据

Redis
    │
    ├── Cache
    ├── Rate Limit
    ├── Chat Memory
    └── Runtime 临时状态

Elasticsearch
    │
    └── Knowledge Chunk / Vector / Keyword

MinIO
    │
    └── PDF / DOCX / TXT / Images / Attachments

---

二、数据库命名规范

数据库：

box

表名统一：

小写 + 下划线

例如：

sys_user
workspace
workspace_member
agent
agent_version
model_provider
model_definition
knowledge_base
knowledge_document
workflow
workflow_version
conversation
conversation_message
agent_execution
trace_span

---

三、所有核心业务表统一字段

建议绝大多数业务表拥有：

id
created_at
updated_at
created_by
updated_by
deleted

例如：

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
created_by BIGINT UNSIGNED NULL,
updated_by BIGINT UNSIGNED NULL,
deleted TINYINT NOT NULL DEFAULT 0

---

四、为什么使用 BIGINT

不要使用：

INT

建议：

BIGINT UNSIGNED

因为 Box 后续可能：

用户
Agent
Conversation
Execution
Trace

数据量都会快速增长。

---

五、主键策略

数据库内部：

BIGINT

API 对外可以使用：

id

或者未来增加：

public_id

例如：

数据库：
100001

API：
agt_01JXYZ...

第一版可以先使用 BIGINT，后续再引入 ULID/UUID 类型的公开 ID。

---

六、用户表

sys_user

CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',

    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) NULL COMMENT '邮箱',
    phone VARCHAR(32) NULL COMMENT '手机号',

    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',

    nickname VARCHAR(64) NULL COMMENT '昵称',
    avatar_url VARCHAR(500) NULL COMMENT '头像',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',

    last_login_at DATETIME NULL COMMENT '最后登录时间',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

密码：

BCrypt / Argon2

绝对不能：

MD5(password)

---

七、Workspace

一个用户可以加入多个工作空间。

CREATE TABLE workspace (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,

    description VARCHAR(500) NULL,
    avatar_url VARCHAR(500) NULL,

    owner_id BIGINT UNSIGNED NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_slug (slug),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

八、Workspace Member

CREATE TABLE workspace_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,

    role_id BIGINT UNSIGNED NULL,

    status TINYINT NOT NULL DEFAULT 1,

    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_workspace_user (
        workspace_id,
        user_id
    ),

    KEY idx_user (user_id),
    KEY idx_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

九、RBAC

角色：

Super Admin
Tenant Admin
Developer
Member

数据库：

sys_role
sys_permission
sys_role_permission
workspace_member

---

十、角色表

CREATE TABLE sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NULL,

    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,

    description VARCHAR(255) NULL,

    built_in TINYINT NOT NULL DEFAULT 0,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id),
    UNIQUE KEY uk_workspace_role (
        workspace_id,
        role_code
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十一、权限表

CREATE TABLE sys_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,

    resource_type VARCHAR(64) NULL,
    action VARCHAR(64) NULL,

    description VARCHAR(255) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_permission_code (
        permission_code
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
knowledge:upload
knowledge:delete

tool:create
tool:execute

model:create
model:update

---

十二、角色权限

CREATE TABLE sys_role_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_role_permission (
        role_id,
        permission_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十三、Agent

这是核心表。

CREATE TABLE agent (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,

    avatar_url VARCHAR(500) NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    published_version_id BIGINT UNSIGNED NULL,

    created_by BIGINT UNSIGNED NOT NULL,
    updated_by BIGINT UNSIGNED NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id),
    KEY idx_created_by (created_by),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十四、Agent Version

这是整个 Agent 配置的核心。

CREATE TABLE agent_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,

    version_no INT NOT NULL,

    version_name VARCHAR(128) NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    system_prompt LONGTEXT NULL,

    model_id BIGINT UNSIGNED NULL,

    temperature DECIMAL(5,4) NULL,
    top_p DECIMAL(5,4) NULL,

    max_tokens INT NULL,

    stream_enabled TINYINT NOT NULL DEFAULT 1,

    memory_enabled TINYINT NOT NULL DEFAULT 1,

    knowledge_enabled TINYINT NOT NULL DEFAULT 0,

    tool_enabled TINYINT NOT NULL DEFAULT 0,

    config_json JSON NULL,

    published_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_agent_version (
        agent_id,
        version_no
    ),

    KEY idx_agent (agent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十五、为什么 Version 很重要

不能直接修改：

published Agent

否则：

用户正在使用 Version 1
开发人员修改配置
突然变成 Version 2

会产生运行时不一致。

正确：

Agent
 ├── Version 1
 ├── Version 2
 ├── Version 3
 └── Version 4 ← Published

运行时永远：

Agent
 ↓
Published Version

---

十六、Agent Variables

CREATE TABLE agent_variable (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,

    variable_name VARCHAR(128) NOT NULL,
    variable_type VARCHAR(32) NOT NULL,

    default_value TEXT NULL,

    required TINYINT NOT NULL DEFAULT 0,

    description VARCHAR(255) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_agent_variable (
        agent_id,
        variable_name
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十七、Model Provider

CREATE TABLE model_provider (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    provider_code VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,

    provider_type VARCHAR(64) NOT NULL,

    base_url VARCHAR(500) NULL,

    status TINYINT NOT NULL DEFAULT 1,

    config_json JSON NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_workspace_provider (
        workspace_id,
        provider_code
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十八、Model Definition

CREATE TABLE model_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    provider_id BIGINT UNSIGNED NOT NULL,

    model_code VARCHAR(128) NOT NULL,
    model_name VARCHAR(128) NOT NULL,

    model_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',

    support_streaming TINYINT NOT NULL DEFAULT 0,
    support_tool_calling TINYINT NOT NULL DEFAULT 0,
    support_vision TINYINT NOT NULL DEFAULT 0,

    context_window INT NULL,
    max_output_tokens INT NULL,

    input_price DECIMAL(18,8) NULL,
    output_price DECIMAL(18,8) NULL,

    config_json JSON NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_provider_model (
        provider_id,
        model_code
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

十九、Model Credential

CREATE TABLE model_credential (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    provider_id BIGINT UNSIGNED NOT NULL,

    credential_name VARCHAR(128) NOT NULL,

    encrypted_api_key TEXT NOT NULL,

    encrypted_secret TEXT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    last_used_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id),
    KEY idx_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十、Knowledge Base

CREATE TABLE knowledge_base (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,

    embedding_model_id BIGINT UNSIGNED NULL,

    retrieval_mode VARCHAR(32) NOT NULL DEFAULT 'HYBRID',

    top_k INT NOT NULL DEFAULT 5,

    score_threshold DECIMAL(8,6) NULL,

    reranker_enabled TINYINT NOT NULL DEFAULT 0,

    status VARCHAR(32) NOT NULL DEFAULT 'READY',

    document_count INT NOT NULL DEFAULT 0,

    chunk_count BIGINT NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十一、Knowledge Document

CREATE TABLE knowledge_document (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    knowledge_base_id BIGINT UNSIGNED NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64) NULL,

    file_size BIGINT NULL,

    minio_bucket VARCHAR(128) NULL,
    minio_object_key VARCHAR(500) NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADING',

    parser_status VARCHAR(32) NULL,
    chunk_status VARCHAR(32) NULL,
    embedding_status VARCHAR(32) NULL,
    indexing_status VARCHAR(32) NULL,

    chunk_count INT NOT NULL DEFAULT 0,

    error_message TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_knowledge_base (
        knowledge_base_id
    ),

    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十二、Knowledge Chunk

注意：

Chunk 正文不建议全部存 MySQL。

因为真正检索的数据应该进入 Elasticsearch。

MySQL 可以保存元数据：

CREATE TABLE knowledge_chunk (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    document_id BIGINT UNSIGNED NOT NULL,

    chunk_index INT NOT NULL,

    content_hash VARCHAR(64) NULL,

    token_count INT NULL,

    page_number INT NULL,

    section_title VARCHAR(500) NULL,

    es_index VARCHAR(128) NULL,
    es_document_id VARCHAR(128) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_document (
        document_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

真正正文：

Elasticsearch

---

二十三、Agent Knowledge Binding

Agent 与知识库是多对多。

CREATE TABLE agent_knowledge_binding (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_version_id BIGINT UNSIGNED NOT NULL,
    knowledge_base_id BIGINT UNSIGNED NOT NULL,

    top_k INT NULL,
    score_threshold DECIMAL(8,6) NULL,

    retrieval_mode VARCHAR(32) NULL,

    reranker_enabled TINYINT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_agent_knowledge (
        agent_version_id,
        knowledge_base_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

这样：

知识库默认配置
+
Agent Version 独立配置

可以覆盖。

---

二十四、Tool

CREATE TABLE tool_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,

    tool_type VARCHAR(32) NOT NULL,

    config_json JSON NOT NULL,

    input_schema JSON NULL,
    output_schema JSON NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id),
    KEY idx_type (tool_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十五、Agent Tool Binding

CREATE TABLE agent_tool_binding (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_version_id BIGINT UNSIGNED NOT NULL,
    tool_id BIGINT UNSIGNED NOT NULL,

    enabled TINYINT NOT NULL DEFAULT 1,

    require_confirmation TINYINT NOT NULL DEFAULT 0,

    config_json JSON NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_agent_tool (
        agent_version_id,
        tool_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十六、MCP Server

后续支持 MCP，因此现在就预留。

CREATE TABLE mcp_server (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,

    server_url VARCHAR(500) NULL,

    transport_type VARCHAR(32) NOT NULL,

    config_json JSON NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DISCONNECTED',

    last_connected_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十七、MCP Tool

CREATE TABLE mcp_tool (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    mcp_server_id BIGINT UNSIGNED NOT NULL,

    tool_name VARCHAR(128) NOT NULL,

    description TEXT NULL,

    input_schema JSON NULL,

    output_schema JSON NULL,

    enabled TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_server_tool (
        mcp_server_id,
        tool_name
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十八、Workflow

CREATE TABLE workflow (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    published_version_id BIGINT UNSIGNED NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_workspace (workspace_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

二十九、Workflow Version

CREATE TABLE workflow_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workflow_id BIGINT UNSIGNED NOT NULL,

    version_no INT NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    definition_json JSON NOT NULL,

    published_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_workflow_version (
        workflow_id,
        version_no
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

三十、为什么 Workflow 使用 JSON

Workflow：

Node
Edge
Config
Position
Variable

天然是树/图结构。

例如：

{
  "nodes": [
    {
      "id": "start",
      "type": "START"
    },
    {
      "id": "llm_1",
      "type": "LLM"
    }
  ],
  "edges": [
    {
      "source": "start",
      "target": "llm_1"
    }
  ]
}

因此第一阶段：

workflow_version.definition_json

非常合适。

不要一开始把：

workflow_node
workflow_edge
workflow_node_config

拆成十几张表。

---

三十一、Conversation

CREATE TABLE conversation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    conversation_id VARCHAR(64) NOT NULL,

    workspace_id BIGINT UNSIGNED NOT NULL,

    agent_id BIGINT UNSIGNED NOT NULL,
    agent_version_id BIGINT UNSIGNED NULL,

    user_id BIGINT UNSIGNED NOT NULL,

    title VARCHAR(255) NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',

    summary TEXT NULL,

    last_message_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_conversation_id (
        conversation_id
    ),

    KEY idx_user_agent (
        user_id,
        agent_id
    ),

    KEY idx_workspace (
        workspace_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

三十二、Conversation Message

这是高增长表。

CREATE TABLE conversation_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    message_id VARCHAR(64) NOT NULL,

    conversation_id BIGINT UNSIGNED NOT NULL,

    role VARCHAR(32) NOT NULL,

    content LONGTEXT NULL,

    content_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',

    tool_calls JSON NULL,

    citations JSON NULL,

    attachments JSON NULL,

    token_count INT NULL,

    sequence_no INT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_message_id (
        message_id
    ),

    KEY idx_conversation_sequence (
        conversation_id,
        sequence_no
    ),

    KEY idx_created_at (
        created_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

三十三、为什么 Message 要 sequence_no

不能完全依赖：

created_at

因为高并发和同毫秒写入可能造成顺序问题。

因此：

sequence_no

用于保证：

1 User
2 Assistant
3 User
4 Assistant

---

三十四、Agent Execution

CREATE TABLE agent_execution (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    execution_id VARCHAR(64) NOT NULL,

    trace_id VARCHAR(64) NOT NULL,

    workspace_id BIGINT UNSIGNED NOT NULL,

    agent_id BIGINT UNSIGNED NOT NULL,
    agent_version_id BIGINT UNSIGNED NOT NULL,

    conversation_id BIGINT UNSIGNED NULL,
    user_id BIGINT UNSIGNED NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',

    input_text LONGTEXT NULL,
    output_text LONGTEXT NULL,

    input_tokens INT NULL,
    output_tokens INT NULL,
    total_tokens INT NULL,

    duration_ms BIGINT NULL,

    error_code VARCHAR(64) NULL,
    error_message TEXT NULL,

    started_at DATETIME NULL,
    finished_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_execution_id (
        execution_id
    ),

    KEY idx_trace (
        trace_id
    ),

    KEY idx_workspace_time (
        workspace_id,
        created_at
    ),

    KEY idx_agent_time (
        agent_id,
        created_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

三十五、Workflow Execution

Workflow 与 Agent Execution 最好分开。

CREATE TABLE workflow_execution (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    execution_id VARCHAR(64) NOT NULL,

    trace_id VARCHAR(64) NOT NULL,

    workspace_id BIGINT UNSIGNED NOT NULL,

    workflow_id BIGINT UNSIGNED NOT NULL,
    workflow_version_id BIGINT UNSIGNED NOT NULL,

    user_id BIGINT UNSIGNED NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',

    input_json JSON NULL,
    output_json JSON NULL,

    duration_ms BIGINT NULL,

    error_code VARCHAR(64) NULL,
    error_message TEXT NULL,

    started_at DATETIME NULL,
    finished_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_execution_id (
        execution_id
    ),

    KEY idx_workspace_time (
        workspace_id,
        created_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

三十六、Trace Span

CREATE TABLE trace_span (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    trace_id VARCHAR(64) NOT NULL,

    span_id VARCHAR(64) NOT NULL,

    parent_span_id VARCHAR(64) NULL,

    execution_id VARCHAR(64) NOT NULL,

    span_type VARCHAR(64) NOT NULL,

    span_name VARCHAR(128) NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',

    start_time DATETIME(3) NULL,
    end_time DATETIME(3) NULL,

    duration_ms BIGINT NULL,

    input_tokens INT NULL,
    output_tokens INT NULL,

    input_data LONGTEXT NULL,
    output_data LONGTEXT NULL,

    error_code VARCHAR(64) NULL,
    error_message TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_span_id (
        span_id
    ),

    KEY idx_trace (
        trace_id
    ),

    KEY idx_execution (
        execution_id
    ),

    KEY idx_parent (
        parent_span_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

三十七、为什么 Trace 不全部放 JSON

虽然：

trace_json

看起来简单。

但是以后前端需要：

按 Span 查询
按 Tool 查询
按 LLM 查询
按耗时排序
统计 Token
统计错误

所以：

trace_span

单独建表更合理。

---

三十八、API Key

CREATE TABLE api_key (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(128) NOT NULL,

    key_prefix VARCHAR(32) NOT NULL,

    key_hash VARCHAR(128) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    last_used_at DATETIME NULL,

    expires_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    created_by BIGINT UNSIGNED NULL,

    deleted TINYINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_key_hash (
        key_hash
    ),

    KEY idx_workspace (
        workspace_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

数据库：

ax_live_xxxxxxxxx

不能保存完整 Key。

只保存：

prefix
+
hash

---

三十九、Published API

CREATE TABLE agent_publish (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    agent_id BIGINT UNSIGNED NOT NULL,

    agent_version_id BIGINT UNSIGNED NOT NULL,

    publish_type VARCHAR(32) NOT NULL,

    public_key VARCHAR(128) NULL,

    status TINYINT NOT NULL DEFAULT 1,

    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    unpublished_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_agent (
        agent_id
    ),

    KEY idx_public_key (
        public_key
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

四十、Audit Log

企业级平台建议从第一版就加入。

CREATE TABLE audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    workspace_id BIGINT UNSIGNED NULL,

    user_id BIGINT UNSIGNED NULL,

    action VARCHAR(128) NOT NULL,

    resource_type VARCHAR(64) NULL,
    resource_id VARCHAR(64) NULL,

    ip VARCHAR(64) NULL,

    user_agent VARCHAR(1000) NULL,

    detail_json JSON NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_workspace_time (
        workspace_id,
        created_at
    ),

    KEY idx_user_time (
        user_id,
        created_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

四十一、Analytics

第一阶段不需要做非常复杂的数据仓库。

可以先使用：

agent_execution
workflow_execution

聚合。

以后数据量大了再考虑：

OLAP
ClickHouse
数据仓库

第一版不要增加。

---

四十二、数据库关系总图

sys_user
   │
   ├──────────────┐
   │              │
   ▼              ▼
workspace     workspace_member
   │
   ├───────────────┬────────────────┬───────────────┐
   │               │                │               │
   ▼               ▼                ▼               ▼
 Agent           Knowledge        Tool           Workflow
   │               │                │               │
   ▼               ▼                ▼               ▼
Agent Version   Document        Tool Binding    Workflow Version
   │               │
   ├──────┐        ▼
   │      │      Chunk
   ▼      ▼
Model   Knowledge Binding
   │
   ▼
Provider
   │
   ▼
Credential

Runtime：

Agent
  │
  ▼
Agent Version
  │
  ▼
Conversation
  │
  ▼
Message
  │
  ▼
Agent Execution
  │
  ├──────► Trace Span
  │
  ├──────► Token Usage
  │
  └──────► Analytics

---

四十三、完整数据库表清单

第一阶段建议最终约：

1.  sys_user

2.  workspace
3.  workspace_member

4.  sys_role
5.  sys_permission
6.  sys_role_permission

7.  agent
8.  agent_version
9.  agent_variable

10. model_provider
11. model_definition
12. model_credential

13. knowledge_base
14. knowledge_document
15. knowledge_chunk
16. agent_knowledge_binding

17. tool_definition
18. agent_tool_binding

19. mcp_server
20. mcp_tool

21. workflow
22. workflow_version

23. conversation
24. conversation_message

25. agent_execution
26. workflow_execution
27. trace_span

28. api_key
29. agent_publish

30. audit_log

30 张左右的核心表是合理的。

不要因为看到 Coze/Dify 有大量表，就一开始照搬几十上百张表。

---

四十四、哪些数据放 MySQL

MySQL
│
├── User
├── Workspace
├── RBAC
├── Agent
├── Agent Version
├── Model
├── Credential Metadata
├── Knowledge Metadata
├── Tool
├── MCP
├── Workflow
├── Conversation
├── Message
├── Execution
├── Trace
├── API Key
└── Audit

---

四十五、哪些数据放 Redis

Redis
│
├── Login Rate Limit
├── API Rate Limit
├── API Key Cache
├── Model Config Cache
├── Agent Published Version Cache
├── Chat Memory
├── Runtime State
└── Distributed Lock（需要时）

例如：

box:agent:{agentId}:published

box:conversation:{conversationId}:memory

box:ratelimit:user:{userId}

---

四十六、哪些数据放 Elasticsearch

知识库：

Elasticsearch
│
└── knowledge_chunk
       │
       ├── document_id
       ├── knowledge_base_id
       ├── content
       ├── title
       ├── page
       ├── keywords
       └── embedding

查询：

用户问题
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

---

四十七、哪些数据放 MinIO

MinIO
│
├── knowledge/
│   ├── pdf
│   ├── docx
│   ├── txt
│   └── md
│
├── avatar/
│
├── attachments/
│
└── workflow/

MySQL：

file_id
bucket
object_key
file_name
size
content_type

MinIO：

真正文件

---

四十八、非常重要：不要让 MySQL 直接存文件

错误：

file_content LONGTEXT

或者：

file_content LONGBLOB

对于 Box 不推荐。

正确：

Browser
 ↓
Spring Boot
 ↓
MinIO
 ↓
MySQL 保存 Metadata

---

四十九、Workspace 数据隔离

这是数据库设计最重要的安全原则之一。

例如 Agent：

agent.workspace_id

Knowledge：

knowledge_base.workspace_id

Tool：

tool_definition.workspace_id

Workflow：

workflow.workspace_id

Execution：

agent_execution.workspace_id

因此任何查询都应该：

WHERE workspace_id = ?

而不是：

WHERE id = ?

---

五十、错误示例

千万不要：

agentMapper.selectById(agentId);

然后直接返回。

因为用户 A 可能传入：

agentId = 用户 B 的 Agent

正确：

SELECT *
FROM agent
WHERE id = ?
  AND workspace_id = ?
  AND deleted = 0;

---

五十一、Resource Authorization

最终权限判断：

User
 ↓
Workspace Membership
 ↓
RBAC Permission
 ↓
Resource Workspace
 ↓
Resource Ownership
 ↓
Operation

例如：

User A
 ↓
Workspace 100
 ↓
agent:update
 ↓
Agent 10001
 ↓
Agent.workspace_id = 100
 ↓
允许

---

五十二、删除策略

大多数业务表：

deleted = 0

软删除。

例如：

Agent
 ↓
deleted = 1

但是：

Conversation Message
Trace
Execution
Audit Log

不建议简单按照普通业务资源做删除。

尤其：

Audit Log

原则上不应该随意删除。

---

五十三、索引原则

不要：

每个字段都加索引

重点索引：

workspace_id
user_id
agent_id
status
created_at
conversation_id
execution_id
trace_id

高频组合：

(workspace_id, created_at)

(conversation_id, sequence_no)

(agent_id, created_at)

---

五十四、Execution 表后期一定会变大

例如：

1000 万次 Agent 执行

因此：

agent_execution
trace_span
conversation_message
audit_log

都是高增长表。

第一版：

MySQL

完全可以。

后期再根据真实数据量进行：

分区
归档
冷热数据
OLAP

不要提前过度架构。

---

五十五、Flyway 数据库版本

建议：

db/migration

结构：

V1__init.sql

V2__create_user.sql

V3__create_workspace.sql

V4__create_rbac.sql

V5__create_agent.sql

V6__create_model.sql

V7__create_knowledge.sql

V8__create_tool.sql

V9__create_workflow.sql

V10__create_conversation.sql

V11__create_runtime.sql

V12__create_trace.sql

V13__create_publish.sql

V14__create_audit.sql

实际开发时可以进一步合并，但不要直接手动修改已经执行过的 Flyway migration。

---

五十六、推荐数据库初始化顺序

01 User
      ↓
02 Workspace
      ↓
03 RBAC
      ↓
04 Agent
      ↓
05 Agent Version
      ↓
06 Model
      ↓
07 Knowledge
      ↓
08 Tool
      ↓
09 Workflow
      ↓
10 Conversation
      ↓
11 Runtime
      ↓
12 Trace
      ↓
13 Publish
      ↓
14 Audit

---

五十七、第一版数据库核心链路

最终只要先跑通下面这条：

sys_user
    ↓
workspace
    ↓
agent
    ↓
agent_version
    ↓
model_provider
    ↓
model_definition
    ↓
model_credential
    ↓
conversation
    ↓
conversation_message
    ↓
agent_execution
    ↓
trace_span

这条链路跑通之后：

«Box 就已经拥有一个真正可运行的 Agent 平台核心骨架。»

---

五十八、最终技术架构

                       Vue 3
                         │
                  TDesign Vue Next
                         │
                         ▼
                       Nginx
                         │
                         ▼
                  Spring Boot 3
                         │
        ┌────────────────┼─────────────────┐
        │                │                 │
        ▼                ▼                 ▼
      MySQL             Redis        Elasticsearch
        │                │                 │
        │                │                 │
        │                │                 ▼
        │                │             Knowledge
        │                │               RAG
        │                │
        │                ▼
        │             Memory
        │             Cache
        │             Rate Limit
        │
        ▼
   Business Data

                         │
                         ▼
                    Agent Runtime
                         │
                         ▼
                     LangChain4j
                         │
                         ▼
                    Model Provider
                         │
             ┌───────────┼───────────┐
             ▼           ▼           ▼
          OpenAI      DeepSeek      Qwen
             │
             ▼
            LLM

                         │
                         ▼
                       MinIO
                         │
                         ▼
                    File Storage

---

五十九、第七部分最终结论

Box 的数据库原则确定为：

MySQL
    = 核心业务数据库

Redis
    = 高速缓存 / Memory / 限流 / 临时状态

Elasticsearch
    = RAG 检索

MinIO
    = 文件存储

LangChain4j
    = AI Runtime

Spring Boot
    = 后端业务平台

Vue 3 + TDesign Vue Next
    = 前端管理平台

同时保持：

模块化单体
+
领域分层
+
Workspace 数据隔离
+
Agent Version
+
Runtime Execution
+
Trace

第一阶段：

❌ Kafka
❌ 微服务
❌ Kubernetes
❌ PostgreSQL
❌ pgvector
❌ ClickHouse

全部暂缓。

这样你的第一版不会变成一个“架构看起来很牛，但半年还跑不起来”的项目。

真正的开发主线应该是：

用户
 ↓
Workspace
 ↓
Agent
 ↓
Agent Version
 ↓
Model
 ↓
LangChain4j
 ↓
Agent Runtime
 ↓
Conversation
 ↓
Trace

然后再逐步接入：

Knowledge
 ↓
RAG
 ↓
Tool
 ↓
Workflow
 ↓
Publish
 ↓
Analytics

这套数据库设计已经可以支撑后面继续开发 Box 的核心功能。