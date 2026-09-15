ALTER TABLE user_preference
    ADD COLUMN current_workspace_id BIGINT UNSIGNED NULL COMMENT '当前工作空间 ID' AFTER send_with_enter;
