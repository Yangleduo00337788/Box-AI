INSERT INTO sys_permission (permission_code, permission_name, resource_type, action) VALUES
('knowledge:read', '查看知识库', 'knowledge', 'read'),
('knowledge:update', '更新知识库', 'knowledge', 'update'),
('workflow:read', '查看工作流', 'workflow', 'read'),
('workflow:delete', '删除工作流', 'workflow', 'delete'),
('tool:update', '更新工具', 'tool', 'update'),
('tool:delete', '删除工具', 'tool', 'delete'),
('member:manage', '管理成员', 'member', 'manage'),
('role:manage', '管理角色', 'role', 'manage'),
('api_key:manage', '管理 API Key', 'api_key', 'manage');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'knowledge:read', 'knowledge:update', 'knowledge:delete',
    'workflow:read', 'workflow:delete',
    'tool:update', 'tool:delete',
    'agent:delete',
    'api_key:manage'
)
WHERE r.built_in = 1 AND r.role_code = 'DEVELOPER' AND r.deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('knowledge:read', 'workflow:read')
WHERE r.built_in = 1 AND r.role_code = 'MEMBER' AND r.deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('member:manage', 'role:manage', 'audit:read')
WHERE r.built_in = 1 AND r.role_code = 'TENANT_ADMIN' AND r.deleted = 0;
