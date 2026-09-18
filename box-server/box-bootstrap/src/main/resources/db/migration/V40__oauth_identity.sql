CREATE TABLE user_oauth_identity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    provider VARCHAR(32) NOT NULL COMMENT 'github/google/sso',
    provider_user_id VARCHAR(128) NOT NULL COMMENT '第三方用户 ID',
    email VARCHAR(128) NULL COMMENT '第三方邮箱快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_provider_uid (provider, provider_user_id),
    KEY idx_oauth_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
