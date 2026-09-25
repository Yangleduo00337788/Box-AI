UPDATE plan
SET audience = 'TEAM'
WHERE name LIKE '%企业%'
   OR name LIKE '%团队%'
   OR LOWER(code) LIKE '%enterprise%'
   OR LOWER(code) LIKE '%team%'
   OR LOWER(code) LIKE '%org%';

UPDATE plan
SET audience = 'PERSONAL'
WHERE name LIKE '%个人%'
   OR LOWER(code) LIKE '%personal%';
