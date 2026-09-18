ALTER TABLE agent_version
    ADD COLUMN routing_preference VARCHAR(16) NOT NULL DEFAULT 'BALANCED'
        COMMENT 'COST/QUALITY/BALANCED，model_source=AUTO 时生效' AFTER model_source;

ALTER TABLE payment_record
    ADD COLUMN notify_payload VARCHAR(4000) NULL COMMENT '支付回调原始摘要' AFTER external_ref;
