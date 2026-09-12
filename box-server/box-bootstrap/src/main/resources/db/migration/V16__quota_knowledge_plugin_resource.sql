ALTER TABLE plan
    ADD COLUMN quota_knowledge_bases INT NOT NULL DEFAULT 0 COMMENT '知识库数量上限，0 表示不限' AFTER quota_workspaces;

UPDATE plan SET quota_knowledge_bases = 3 WHERE code = 'personal_free';
UPDATE plan SET quota_knowledge_bases = 100 WHERE code = 'enterprise_starter';

ALTER TABLE workspace_plugin_install
    ADD COLUMN resource_type VARCHAR(32) NULL COMMENT '安装后创建的资源类型' AFTER plugin_id,
    ADD COLUMN resource_id BIGINT UNSIGNED NULL COMMENT '安装后创建的资源 ID' AFTER resource_type;
