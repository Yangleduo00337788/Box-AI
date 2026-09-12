ALTER TABLE execution
    ADD COLUMN request_id VARCHAR(64) DEFAULT NULL AFTER execution_no,
    ADD KEY idx_request_id (request_id);
