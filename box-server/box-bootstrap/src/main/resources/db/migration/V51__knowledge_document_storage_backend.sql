ALTER TABLE knowledge_document
    ADD COLUMN storage_backend VARCHAR(32) NULL COMMENT '对象存储后端：MINIO / R2，空表示历史 MinIO' AFTER storage_key;
