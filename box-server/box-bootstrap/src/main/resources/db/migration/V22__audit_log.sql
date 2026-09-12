CREATE TABLE audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NULL,
    user_id BIGINT UNSIGNED NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(64) NULL,
    resource_name VARCHAR(255) NULL,
    result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    ip_address VARCHAR(64) NULL,
    trace_id VARCHAR(64) NULL,
    detail VARCHAR(1024) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_workspace_time (workspace_id, created_at),
    KEY idx_audit_user_time (user_id, created_at),
    KEY idx_audit_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

INSERT INTO sys_permission (permission_code, permission_name, resource_type, action) VALUES
('audit:read', '查看审计日志', 'audit', 'read');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'audit:read'
WHERE r.built_in = 1 AND r.role_code = 'TENANT_ADMIN' AND r.deleted = 0;
