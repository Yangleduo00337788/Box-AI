-- 清理自动化/手工 E2E 产生的临时套餐（无租户引用时可物理删除）
DELETE p
FROM plan p
         LEFT JOIN tenant t ON t.plan_id = p.id AND t.deleted = 0
WHERE t.id IS NULL
  AND (
    LOWER(p.code) LIKE 'e2e\_%'
        OR p.name LIKE 'E2E-%'
        OR p.name LIKE 'E2E\_%'
    );
