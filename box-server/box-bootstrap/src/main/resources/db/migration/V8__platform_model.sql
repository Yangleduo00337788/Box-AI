CREATE TABLE platform_provider (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_code VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(64) NOT NULL DEFAULT 'OPENAI_COMPATIBLE',
    base_url VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_code (provider_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台模型服务商';

CREATE TABLE platform_model (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_id BIGINT UNSIGNED NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,
    model_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',
    support_streaming TINYINT NOT NULL DEFAULT 1,
    context_window INT NULL,
    max_output_tokens INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_model (provider_id, model_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台模型';

CREATE TABLE platform_credential (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_id BIGINT UNSIGNED NOT NULL,
    credential_name VARCHAR(128) NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台模型密钥';

ALTER TABLE plan
    ADD COLUMN byok_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许自带密钥' AFTER quota_workspaces;

UPDATE plan SET byok_enabled = 1 WHERE code = 'enterprise_pro';

ALTER TABLE agent_version
    ADD COLUMN platform_model_id BIGINT UNSIGNED NULL AFTER model_id,
    ADD COLUMN model_source VARCHAR(32) NOT NULL DEFAULT 'PLATFORM' COMMENT 'PLATFORM/BYOK' AFTER platform_model_id;

INSERT INTO platform_provider (provider_code, provider_name, provider_type, base_url, status)
VALUES ('openai', 'OpenAI', 'OPENAI_COMPATIBLE', 'https://api.openai.com/v1', 1);

INSERT INTO platform_model (provider_id, model_code, model_name, description, support_streaming, context_window, max_output_tokens, sort_order, status)
SELECT p.id, 'gpt-4o-mini', 'GPT-4o Mini', '高性价比对话模型', 1, 128000, 16384, 10, 1
FROM platform_provider p WHERE p.provider_code = 'openai';

INSERT INTO platform_model (provider_id, model_code, model_name, description, support_streaming, context_window, max_output_tokens, sort_order, status)
SELECT p.id, 'gpt-4o', 'GPT-4o', '更强推理与多模态能力', 1, 128000, 16384, 20, 1
FROM platform_provider p WHERE p.provider_code = 'openai';
