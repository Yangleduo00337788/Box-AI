CREATE TABLE plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    price_monthly DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '月费，元',
    quota_ai_calls INT NOT NULL DEFAULT 0 COMMENT '每月 AI 调用次数，0 表示不限',
    quota_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '每月 Token 用量，0 表示不限',
    quota_members INT NOT NULL DEFAULT 0 COMMENT '成员数上限，0 表示不限',
    quota_workspaces INT NOT NULL DEFAULT 0 COMMENT '工作空间数上限，0 表示不限',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐方案';

CREATE TABLE tenant_usage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT UNSIGNED NOT NULL,
    period CHAR(7) NOT NULL COMMENT 'YYYY-MM',
    ai_calls INT NOT NULL DEFAULT 0,
    tokens BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_period (tenant_id, period),
    KEY idx_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户月度用量';

ALTER TABLE tenant
    ADD COLUMN plan_id BIGINT UNSIGNED NULL AFTER tenant_type;

INSERT INTO plan (code, name, description, price_monthly, quota_ai_calls, quota_tokens, quota_members, quota_workspaces, status)
VALUES
    ('personal_free', '个人免费版', '适合个人创作者体验', 0, 100, 50000, 1, 1, 1),
    ('enterprise_starter', '企业入门版', '适合小团队协作', 99, 1000, 500000, 10, 3, 1),
    ('enterprise_pro', '企业专业版', '适合中大型团队', 499, 10000, 5000000, 50, 10, 1);

UPDATE tenant t
JOIN plan p ON p.code = 'personal_free'
SET t.plan_id = p.id
WHERE t.tenant_type = 'PERSONAL' AND t.plan_id IS NULL;

UPDATE tenant t
JOIN plan p ON p.code = 'enterprise_starter'
SET t.plan_id = p.id
WHERE t.tenant_type = 'ENTERPRISE' AND t.plan_id IS NULL;

UPDATE tenant t
JOIN plan p ON p.code = 'enterprise_starter'
SET t.plan_id = p.id
WHERE t.plan_id IS NULL;
