ALTER TABLE knowledge_base
    ADD COLUMN ocr_model_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'OCR/视觉模型（平台模型池 ID），空则自动选择' AFTER rerank_model_id;
