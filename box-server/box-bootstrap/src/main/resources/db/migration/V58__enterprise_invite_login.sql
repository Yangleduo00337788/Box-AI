ALTER TABLE tenant
    ADD COLUMN invite_code VARCHAR(32) NULL COMMENT '企业邀请码' AFTER contact_email;

ALTER TABLE tenant
    ADD UNIQUE KEY uk_tenant_invite_code (invite_code);

ALTER TABLE tenant_member
    ADD COLUMN login_name VARCHAR(64) NULL COMMENT '企业内登录账号' AFTER role_code;

ALTER TABLE tenant_member
    ADD UNIQUE KEY uk_tenant_login_name (tenant_id, login_name);

UPDATE tenant
SET invite_code = UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 10))
WHERE tenant_type = 'ENTERPRISE'
  AND (invite_code IS NULL OR invite_code = '');

UPDATE tenant_member tm
JOIN sys_user u ON u.id = tm.user_id AND u.deleted = 0
SET tm.login_name = LOWER(IFNULL(NULLIF(u.email, ''), u.username))
WHERE tm.login_name IS NULL
  AND tm.deleted = 0;
