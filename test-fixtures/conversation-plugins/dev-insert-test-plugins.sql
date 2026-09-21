-- 本地手工测试：对话加号插件（Tool / Skill / MCP）
-- 在 MySQL box 库执行一次即可；若 plugin_code 已存在则更新为上架状态并刷新 manifest。
-- 执行后：插件市场安装 → 对话页加号勾选 → 发消息验证。

INSERT INTO plugin_catalog (
    plugin_code, category, title, description, manifest_json,
    status, review_status, source_type, visibility, rollout_percent, sort_order, install_count, deleted, created_at, updated_at
) VALUES
(
    'fixture-chat-tool-httpbin',
    'tools',
    '[测试] HTTPBin GET',
    '对话加号测试：GET https://httpbin.org/get，用于验证 Tool Calling 是否成功。',
    '{"url":"https://httpbin.org/get","method":"GET","type":"HTTP","timeoutMs":15000,"allowRedirect":false}',
    'LISTED', 'APPROVED', 'ADMIN', 'GLOBAL', 100, 910, 0, 0, NOW(3), NOW(3)
),
(
    'fixture-chat-skill-brief',
    'skills',
    '[测试] 三条要点 Skill',
    '对话加号测试：启用后回答必须带【技能已启用】与三条 • 要点。',
    '{"instructions":"你是「三条要点」技能。无论用户问什么：\\n1. 第一行必须单独输出：【技能已启用】\\n2. 随后用恰好三条要点回答，每条以「•」开头，每条不超过 30 个汉字。\\n3. 不要输出代码块，不要调用工具，不要编造具体价格或合同条款。"}',
    'LISTED', 'APPROVED', 'ADMIN', 'GLOBAL', 100, 920, 0, 0, NOW(3), NOW(3)
),
(
    'fixture-chat-mcp-echo',
    'mcp',
    '[测试] MCP Echo',
    '对话加号测试：需本机 MCP 端点（见 README）；工具名 echo。',
    '{"endpointUrl":"http://127.0.0.1:3100/mcp","transportType":"HTTP","authType":"NONE","toolCatalogJson":"[{\\"name\\":\\"echo\\",\\"description\\":\\"回显 message 参数\\"}]"}',
    'LISTED', 'APPROVED', 'ADMIN', 'GLOBAL', 100, 930, 0, 0, NOW(3), NOW(3)
)
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    description = VALUES(description),
    manifest_json = VALUES(manifest_json),
    status = 'LISTED',
    review_status = 'APPROVED',
    source_type = 'ADMIN',
    visibility = 'GLOBAL',
    rollout_percent = 100,
    deleted = 0,
    updated_at = NOW(3);
