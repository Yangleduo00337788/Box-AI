CREATE TABLE IF NOT EXISTS workspace_invitation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    email VARCHAR(255) NOT NULL,
    role_code VARCHAR(64) NOT NULL DEFAULT 'MEMBER',
    token VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/EXPIRED/REVOKED',
    invited_by BIGINT UNSIGNED NOT NULL,
    expires_at DATETIME NOT NULL,
    accepted_at DATETIME NULL,
    accepted_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_token (token),
    KEY idx_invitation_workspace (workspace_id),
    KEY idx_invitation_email (email),
    KEY idx_invitation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作空间邀请';

CREATE TABLE IF NOT EXISTS user_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    device_name VARCHAR(128) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    last_active_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_session_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录会话';

ALTER TABLE sys_user
    ADD COLUMN platform_admin_role VARCHAR(32) NULL
        COMMENT 'SUPER_ADMIN/OPS/FINANCE/CONTENT，仅 PLATFORM_ADMIN 有效' AFTER user_type;

UPDATE sys_user SET platform_admin_role = 'SUPER_ADMIN' WHERE user_type = 'PLATFORM_ADMIN' AND platform_admin_role IS NULL;
