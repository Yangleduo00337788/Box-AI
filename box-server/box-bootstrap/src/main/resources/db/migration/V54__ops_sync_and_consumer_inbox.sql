ALTER TABLE ops_placement
    ADD COLUMN sync_peer_id BIGINT UNSIGNED NULL COMMENT 'C/B 同步对端运营位 ID' AFTER audience,
    ADD KEY idx_ops_placement_sync_peer (sync_peer_id);

CREATE TABLE consumer_inbox_read (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,
    notice_key VARCHAR(128) NOT NULL COMMENT '如 ops:123',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_consumer_inbox_read (user_id, workspace_id, notice_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C 端站内信运营公告已读';

CREATE TABLE consumer_inbox_dismiss (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,
    notice_key VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_consumer_inbox_dismiss (user_id, workspace_id, notice_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C 端站内信运营公告已关闭';
