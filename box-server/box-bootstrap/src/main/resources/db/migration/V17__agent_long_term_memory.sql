ALTER TABLE agent_version
    ADD COLUMN long_term_memory_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用跨会话长期记忆' AFTER memory_window_size;

CREATE TABLE agent_long_term_memory (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    agent_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    content TEXT NOT NULL,
    es_document_id VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_agent_user (agent_id, user_id),
    KEY idx_workspace (workspace_id),
    UNIQUE KEY uk_es_document_id (es_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 长期记忆';
