ALTER TABLE message_feedback
    ADD COLUMN status VARCHAR(16) NULL COMMENT 'PENDING|REPLIED，仅 bad 使用' AFTER content,
    ADD COLUMN admin_reply TEXT NULL COMMENT '平台管理员回复' AFTER status,
    ADD COLUMN admin_reply_by BIGINT UNSIGNED NULL AFTER admin_reply,
    ADD COLUMN admin_replied_at DATETIME NULL AFTER admin_reply_by,
    ADD KEY idx_message_feedback_status_created (status, created_at);
