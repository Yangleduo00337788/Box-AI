CREATE TABLE platform_admin_inbox_read (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '平台管理员用户 ID',
    notice_key VARCHAR(128) NOT NULL COMMENT '通知键',
    read_marker VARCHAR(64) DEFAULT NULL COMMENT '待办类：已读时的数量快照，有新待办时重新未读',
    read_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_admin_inbox_read (admin_user_id, notice_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台管理员站内通知已读记录';
