INSERT INTO system_config (config_key, config_value, description)
VALUES ('platform.ocr.default_model_id', '', '平台默认 OCR/视觉模型（platform_model.id），供知识库图片与扫描 PDF 识别')
ON DUPLICATE KEY UPDATE description = VALUES(description);
