-- 停用旧演示套餐，上线正式个人/团队三档 + 企业定制（仅 B 端分配 / C 端留资）
UPDATE plan SET status = 0, updated_at = NOW()
WHERE code IN ('enterprise_starter', 'enterprise_pro');

UPDATE plan
SET name = '免费版',
    description = '个人体验 Builder、对话与轻量知识库',
    price_monthly = 0,
    quota_ai_calls = 100,
    quota_tokens = 50000,
    quota_members = 1,
    quota_workspaces = 1,
    quota_knowledge_bases = 3,
    audience = 'PERSONAL',
    overage_policy = 'REJECT',
    byok_enabled = 0,
    status = 1,
    updated_at = NOW()
WHERE code = 'personal_free';

INSERT INTO plan (code, name, description, audience, price_monthly, quota_ai_calls, quota_tokens,
                  quota_members, quota_workspaces, quota_knowledge_bases, overage_policy, byok_enabled, status)
SELECT 'personal_pro', '专业版', '个人深度使用：更高 Token 与知识库配额', 'PERSONAL', 69.00,
       2000, 500000, 1, 2, 10, 'REJECT', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'personal_pro');

INSERT INTO plan (code, name, description, audience, price_monthly, quota_ai_calls, quota_tokens,
                  quota_members, quota_workspaces, quota_knowledge_bases, overage_policy, byok_enabled, status)
SELECT 'team_starter', '团队入门', '小团队协作：多成员与工作空间', 'TEAM', 199.00,
       10000, 2000000, 10, 3, 20, 'REJECT', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'team_starter');

INSERT INTO plan (code, name, description, audience, price_monthly, quota_ai_calls, quota_tokens,
                  quota_members, quota_workspaces, quota_knowledge_bases, overage_policy, byok_enabled, status)
SELECT 'team_business', '团队商业', '中大型团队：高配额与自带模型密钥', 'TEAM', 999.00,
       50000, 10000000, 50, 10, 100, 'DEGRADE', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'team_business');

INSERT INTO plan (code, name, description, audience, price_monthly, quota_ai_calls, quota_tokens,
                  quota_members, quota_workspaces, quota_knowledge_bases, overage_policy, byok_enabled, status)
SELECT 'team_enterprise', '企业定制', '私有化、SLA 与专属配额，请联系销售', 'TEAM', 0.00,
       0, 0, 0, 0, 0, 'REJECT', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'team_enterprise');

-- 租户套餐重绑：个人 → 免费版；原企业入门 → 团队入门；原企业专业 → 团队商业
UPDATE tenant t
    INNER JOIN plan p ON t.plan_id = p.id
SET t.plan_id = (SELECT id FROM plan WHERE code = 'personal_free' AND status = 1 LIMIT 1)
WHERE t.tenant_type = 'PERSONAL'
  AND (p.code IN ('enterprise_starter', 'enterprise_pro', 'team_starter', 'team_business', 'team_enterprise')
    OR p.status = 0);

UPDATE tenant t
    INNER JOIN plan p ON t.plan_id = p.id
SET t.plan_id = (SELECT id FROM plan WHERE code = 'team_business' AND status = 1 LIMIT 1)
WHERE p.code = 'enterprise_pro';

UPDATE tenant t
    INNER JOIN plan p ON t.plan_id = p.id
SET t.plan_id = (SELECT id FROM plan WHERE code = 'team_starter' AND status = 1 LIMIT 1)
WHERE t.tenant_type = 'ENTERPRISE'
  AND (p.code IN ('enterprise_starter', 'enterprise_pro') OR p.status = 0 OR t.plan_id IS NULL);

UPDATE tenant t
SET t.plan_id = (SELECT id FROM plan WHERE code = 'personal_free' AND status = 1 LIMIT 1)
WHERE t.tenant_type = 'PERSONAL' AND t.plan_id IS NULL;

UPDATE tenant t
SET t.plan_id = (SELECT id FROM plan WHERE code = 'team_starter' AND status = 1 LIMIT 1)
WHERE t.tenant_type = 'ENTERPRISE' AND t.plan_id IS NULL;

-- 订阅与账单中的套餐 ID 同步（仅仍指向已停用套餐的记录）
UPDATE subscription s
    INNER JOIN plan p ON s.plan_id = p.id
SET s.plan_id = (SELECT id FROM plan WHERE code = 'team_business' AND status = 1 LIMIT 1)
WHERE p.code = 'enterprise_pro';

UPDATE subscription s
    INNER JOIN plan p ON s.plan_id = p.id
SET s.plan_id = (SELECT id FROM plan WHERE code = 'team_starter' AND status = 1 LIMIT 1)
WHERE p.code = 'enterprise_starter';

UPDATE billing_invoice bi
    INNER JOIN plan p ON bi.plan_id = p.id
SET bi.plan_id = (SELECT id FROM plan WHERE code = 'team_business' AND status = 1 LIMIT 1)
WHERE p.code = 'enterprise_pro';

UPDATE billing_invoice bi
    INNER JOIN plan p ON bi.plan_id = p.id
SET bi.plan_id = (SELECT id FROM plan WHERE code = 'team_starter' AND status = 1 LIMIT 1)
WHERE p.code = 'enterprise_starter';
