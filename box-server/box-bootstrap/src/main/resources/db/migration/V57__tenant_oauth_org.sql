CREATE TABLE tenant_oauth_org (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '企业租户 ID',
    provider VARCHAR(32) NOT NULL COMMENT 'feishu/dingtalk',
    org_id VARCHAR(128) NOT NULL COMMENT '飞书 tenant_key / 钉钉 corpId',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_org (provider, org_id),
    KEY idx_tenant_oauth_org_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业租户与飞书/钉钉组织绑定';
