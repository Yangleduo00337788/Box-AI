ALTER TABLE plan
    ADD COLUMN audience VARCHAR(16) NOT NULL DEFAULT 'PERSONAL' COMMENT 'PERSONAL 个人工作 / TEAM 团队协作' AFTER description;

UPDATE plan
SET audience = 'TEAM'
WHERE LOWER(code) LIKE '%enterprise%'
   OR LOWER(code) LIKE '%team%'
   OR LOWER(code) LIKE '%org%'
   OR name LIKE '%企业%'
   OR name LIKE '%团队%'
   OR quota_members > 5;
