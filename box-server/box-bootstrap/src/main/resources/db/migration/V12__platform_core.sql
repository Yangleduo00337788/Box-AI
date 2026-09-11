-- Knowledge
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';

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
    UNIQUE KEY uk_es_document_id (es_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文本Chunk';

-- Tool
CREATE TABLE tool (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    tool_key VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    type VARCHAR(32) NOT NULL,
    input_schema JSON DEFAULT NULL,
    output_schema JSON DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_tool_key (workspace_id, tool_key),
    KEY idx_workspace_id (workspace_id),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Tool';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HTTP Tool配置';

-- Agent bindings
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
    UNIQUE KEY uk_version_knowledge (version_id, knowledge_base_id),
    KEY idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent知识库关系';

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
    UNIQUE KEY uk_version_tool (version_id, tool_id),
    KEY idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具关系';

-- Workflow
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow';

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
    UNIQUE KEY uk_workflow_version (workflow_id, version_no),
    KEY idx_workspace_id (workspace_id),
    KEY idx_workflow_id (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow版本';

-- Execution & Trace
CREATE TABLE execution (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    execution_no VARCHAR(64) NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,
    execution_type VARCHAR(32) NOT NULL,
    agent_id BIGINT UNSIGNED DEFAULT NULL,
    agent_version_id BIGINT UNSIGNED DEFAULT NULL,
    workflow_id BIGINT UNSIGNED DEFAULT NULL,
    workflow_version_id BIGINT UNSIGNED DEFAULT NULL,
    conversation_id BIGINT UNSIGNED DEFAULT NULL,
    user_id BIGINT UNSIGNED DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input_json JSON DEFAULT NULL,
    output_json JSON DEFAULT NULL,
    error_code VARCHAR(128) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    started_at DATETIME(3) DEFAULT NULL,
    finished_at DATETIME(3) DEFAULT NULL,
    duration_ms BIGINT DEFAULT NULL,
    total_tokens INT DEFAULT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_execution_no (execution_no),
    KEY idx_workspace_id (workspace_id),
    KEY idx_agent_id (agent_id),
    KEY idx_workflow_id (workflow_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent/Workflow执行记录';

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
    UNIQUE KEY uk_trace_id (trace_id),
    KEY idx_execution_id (execution_id),
    KEY idx_workspace_id (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行Trace';

CREATE TABLE trace_span (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    span_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64) DEFAULT NULL,
    span_type VARCHAR(32) NOT NULL,
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
    UNIQUE KEY uk_span_id (span_id),
    KEY idx_trace_id (trace_id),
    KEY idx_span_type (span_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trace Span';

-- Publish & API Key
CREATE TABLE publish (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    published_by BIGINT UNSIGNED NOT NULL,
    published_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_workspace_id (workspace_id),
    KEY idx_resource (resource_type, resource_id),
    KEY idx_version_id (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布记录';

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
    UNIQUE KEY uk_key_hash (key_hash),
    KEY idx_workspace_id (workspace_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放API Key';
