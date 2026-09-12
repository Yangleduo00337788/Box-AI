ALTER TABLE workflow
    ADD COLUMN webhook_token VARCHAR(64) DEFAULT NULL COMMENT 'Webhook 触发令牌' AFTER published_version_id,
    ADD COLUMN webhook_secret VARCHAR(128) DEFAULT NULL COMMENT 'Webhook 签名密钥' AFTER webhook_token,
    ADD UNIQUE KEY uk_webhook_token (webhook_token);
