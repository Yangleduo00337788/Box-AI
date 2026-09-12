CREATE TABLE notification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1024) NOT NULL,
    category VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    link_url VARCHAR(512) DEFAULT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notification_user (workspace_id, user_id, read_flag, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知';
