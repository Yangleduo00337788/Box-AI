ALTER TABLE plugin_catalog
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN 平台上架 / USER 用户投稿' AFTER review_status,
    ADD COLUMN submitted_by BIGINT UNSIGNED NULL COMMENT 'C 端投稿用户' AFTER source_type,
    ADD COLUMN submitted_workspace_id BIGINT UNSIGNED NULL COMMENT 'C 端投稿工作空间' AFTER submitted_by;

ALTER TABLE plugin_catalog
    ADD KEY idx_plugin_submitted_by (submitted_by);

-- 下架初始化种子与 E2E 脏数据，市场仅保留后台上架或用户投稿且已审核的插件
UPDATE plugin_catalog
SET status = 'UNLISTED',
    updated_at = NOW()
WHERE deleted = 0
  AND (
        plugin_code IN (
            'wf-1', 'wf-2', 'wf-3',
            'kb-1', 'kb-2', 'kb-3',
            'tool-1', 'tool-2', 'tool-3',
            'mcp-1', 'mcp-2', 'mcp-3',
            'skill-code-review', 'skill-writing-brief', 'skill-data-report'
        )
        OR plugin_code LIKE 'E2E%'
        OR title LIKE 'E2E-%'
    );
