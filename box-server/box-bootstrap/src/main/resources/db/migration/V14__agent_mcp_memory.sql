CREATE TABLE agent_mcp (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    agent_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,
    mcp_server_id BIGINT UNSIGNED NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_version_mcp (version_id, mcp_server_id),
    KEY idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent MCP 关系';

ALTER TABLE agent_version
    ADD COLUMN memory_window_size INT NOT NULL DEFAULT 20 AFTER memory_enabled;
