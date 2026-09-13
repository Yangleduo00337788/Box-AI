CREATE TABLE embed_custom_domain (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    agent_id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(255) NOT NULL,
    verify_token VARCHAR(64) NOT NULL,
    verified TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_embed_domain (domain),
    KEY idx_embed_agent (agent_id),
    KEY idx_embed_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
