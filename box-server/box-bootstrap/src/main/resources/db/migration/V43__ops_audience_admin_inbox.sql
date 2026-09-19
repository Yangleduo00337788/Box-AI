ALTER TABLE ops_placement
    ADD COLUMN audience CHAR(1) NOT NULL DEFAULT 'C' COMMENT 'C=消费者端 B=管理后台' AFTER id;

CREATE INDEX idx_ops_placement_audience_slot ON ops_placement (audience, slot, status, sort_order);

CREATE TABLE platform_admin_inbox_dismiss (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '平台管理员用户 ID',
    notice_key VARCHAR(128) NOT NULL COMMENT '通知键，如 ops:123',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_admin_inbox_dismiss (admin_user_id, notice_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台管理员站内通知已关闭记录';
