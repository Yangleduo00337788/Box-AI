INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.built_in = 1 AND r.role_code = 'TENANT_ADMIN' AND r.deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'agent:create', 'agent:read', 'agent:update', 'agent:publish',
    'workflow:create', 'workflow:update', 'workflow:execute',
    'knowledge:create', 'knowledge:upload',
    'tool:create', 'tool:execute',
    'model:create', 'model:update'
)
WHERE r.built_in = 1 AND r.role_code = 'DEVELOPER' AND r.deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('agent:read', 'workflow:execute', 'tool:execute')
WHERE r.built_in = 1 AND r.role_code = 'MEMBER' AND r.deleted = 0;
