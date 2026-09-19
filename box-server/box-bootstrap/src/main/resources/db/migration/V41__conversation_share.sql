CREATE TABLE conversation_share (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(64)     NOT NULL COMMENT '公开访问令牌',
    workspace_id    BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    payload_json    JSON            NOT NULL COMMENT '分享快照',
    created_by      BIGINT UNSIGNED NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_share_token (token),
    KEY idx_conversation_share_conversation (conversation_id)
) COMMENT='会话分享';

CREATE TABLE message_feedback (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workspace_id    BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NOT NULL,
    message_id      BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    rating          VARCHAR(16)     NOT NULL COMMENT 'good|bad',
    content         TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_message_feedback_message (message_id)
) COMMENT='消息反馈';
