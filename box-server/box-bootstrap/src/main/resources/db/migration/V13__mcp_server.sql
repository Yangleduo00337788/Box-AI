-- MCP Server configuration
CREATE TABLE mcp_server (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    server_key VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    transport_type VARCHAR(32) NOT NULL DEFAULT 'SSE',
    endpoint_url VARCHAR(1024) NOT NULL,
    auth_type VARCHAR(32) DEFAULT 'NONE',
    auth_config JSON DEFAULT NULL,
    tool_catalog_json JSON DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_sync_at DATETIME(3) DEFAULT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_server_key (workspace_id, server_key),
    KEY idx_workspace_id (workspace_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Server 配置';
