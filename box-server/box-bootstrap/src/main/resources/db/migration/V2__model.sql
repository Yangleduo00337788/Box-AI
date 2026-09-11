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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_provider (workspace_id, provider_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_model (provider_id, model_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_workspace (workspace_id),
    KEY idx_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
