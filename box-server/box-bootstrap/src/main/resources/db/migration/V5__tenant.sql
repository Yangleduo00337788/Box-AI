CREATE TABLE tenant (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    contact_email VARCHAR(128) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
    owner_id BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slug (slug),
    KEY idx_status (status),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业租户';

CREATE TABLE tenant_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role_code VARCHAR(64) NOT NULL DEFAULT 'TENANT_ADMIN',
    status TINYINT NOT NULL DEFAULT 1,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    KEY idx_user (user_id),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员';

ALTER TABLE sys_user
    ADD COLUMN user_type VARCHAR(32) NOT NULL DEFAULT 'TENANT_USER' COMMENT 'PLATFORM_ADMIN/TENANT_USER' AFTER status;

ALTER TABLE workspace
    ADD COLUMN tenant_id BIGINT UNSIGNED NULL AFTER id;

INSERT INTO tenant (id, name, slug, contact_email, status, owner_id, created_at, updated_at, deleted)
SELECT 1, 'Box 官方', 'box-official', 'box@box.ai', 1, MIN(id), NOW(), NOW(), 0
FROM sys_user
WHERE deleted = 0
LIMIT 1;

UPDATE workspace SET tenant_id = 1 WHERE tenant_id IS NULL;

INSERT INTO tenant_member (tenant_id, user_id, role_code, status, joined_at, created_at, updated_at, deleted)
SELECT DISTINCT 1, wm.user_id, 'TENANT_ADMIN', 1, NOW(), NOW(), NOW(), 0
FROM workspace_member wm
WHERE wm.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM tenant_member tm WHERE tm.tenant_id = 1 AND tm.user_id = wm.user_id AND tm.deleted = 0
  );

ALTER TABLE workspace
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL;

INSERT INTO sys_user (username, email, password_hash, nickname, status, user_type, created_at, updated_at, deleted)
SELECT 'admin@box.com', 'admin@box.com', '$2b$10$kZhYhVVBATkXLttM3cMnnu0P6lpFcFaW.GruzWQvNjD7Ew2PORzJ.', '平台管理员', 1, 'PLATFORM_ADMIN', NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'admin@box.com' AND deleted = 0);
