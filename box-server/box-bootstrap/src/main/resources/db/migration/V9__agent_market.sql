CREATE TABLE agent_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,
    avatar_url VARCHAR(500) NULL,
    category VARCHAR(64) NULL,
    system_prompt LONGTEXT NULL,
    platform_model_id BIGINT UNSIGNED NULL,
    temperature DECIMAL(5,4) NULL,
    top_p DECIMAL(5,4) NULL,
    max_tokens INT NULL,
    stream_enabled TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/LISTED/ARCHIVED',
    sort_order INT NOT NULL DEFAULT 0,
    install_count INT NOT NULL DEFAULT 0,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code),
    KEY idx_status (status),
    KEY idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体市场模板';

ALTER TABLE agent
    ADD COLUMN source_template_id BIGINT UNSIGNED NULL COMMENT '来源市场模板' AFTER avatar_url;

INSERT INTO agent_template (
    template_code, name, description, category, system_prompt,
    platform_model_id, temperature, top_p, max_tokens, stream_enabled,
    status, sort_order
)
SELECT
    'writing-assistant',
    '写作助手',
    '帮你润色文案、改写段落、生成标题与摘要',
    '创作',
    '你是一位专业的中文写作助手，擅长润色、改写与结构化表达。回答简洁清晰，保留用户原意。',
    m.id,
    0.7000,
    1.0000,
    4096,
    1,
    'LISTED',
    10
FROM platform_model m
WHERE m.model_code = 'gpt-4o-mini'
LIMIT 1;

INSERT INTO agent_template (
    template_code, name, description, category, system_prompt,
    platform_model_id, temperature, top_p, max_tokens, stream_enabled,
    status, sort_order
)
SELECT
    'code-reviewer',
    '代码审查助手',
    '审查代码质量、发现潜在问题并给出改进建议',
    '开发',
    '你是一位资深软件工程师，擅长代码审查。指出问题时要说明原因，并给出可执行的改进建议。',
    m.id,
    0.3000,
    1.0000,
    4096,
    1,
    'LISTED',
    20
FROM platform_model m
WHERE m.model_code = 'gpt-4o-mini'
LIMIT 1;
