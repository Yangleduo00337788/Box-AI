ALTER TABLE agent_template
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING_REVIEW/APPROVED/REJECTED' AFTER status,
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/TENANT' AFTER review_status,
    ADD COLUMN tenant_ids_json VARCHAR(2000) NULL COMMENT 'visibility=TENANT 时租户 ID 列表 JSON' AFTER visibility,
    ADD COLUMN rollout_percent INT NOT NULL DEFAULT 100 COMMENT '灰度百分比 0-100' AFTER tenant_ids_json;

ALTER TABLE plugin_catalog
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED' AFTER status,
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'GLOBAL' AFTER review_status,
    ADD COLUMN tenant_ids_json VARCHAR(2000) NULL AFTER visibility,
    ADD COLUMN rollout_percent INT NOT NULL DEFAULT 100 AFTER tenant_ids_json;

CREATE TABLE ops_placement_metric (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    placement_id BIGINT UNSIGNED NOT NULL,
    metric_date DATE NOT NULL,
    impressions INT NOT NULL DEFAULT 0,
    clicks INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_placement_date (placement_id, metric_date),
    KEY idx_metric_date (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营位效果';

ALTER TABLE knowledge_document
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/URL' AFTER status,
    ADD COLUMN source_url VARCHAR(1024) NULL AFTER source_type,
    ADD COLUMN sync_cron VARCHAR(64) NULL COMMENT 'URL 定时同步 cron' AFTER source_url,
    ADD COLUMN last_synced_at DATETIME NULL AFTER sync_cron;
