ALTER TABLE knowledge_document
    ADD COLUMN progress INT NOT NULL DEFAULT 0 COMMENT '处理进度 0-100' AFTER status;

UPDATE knowledge_document SET progress = 100 WHERE status = 'READY';
UPDATE knowledge_document SET progress = 10 WHERE status IN ('PARSING', 'OCR', 'CHUNKING', 'EMBEDDING', 'INDEXING');
