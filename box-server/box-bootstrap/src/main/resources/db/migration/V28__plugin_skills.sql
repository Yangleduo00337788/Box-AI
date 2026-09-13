INSERT INTO plugin_category (category_code, label, description, sort_order, status)
SELECT 'skills',
       'Skills',
       '可安装的 Agent 技能包：按约定提供指令与工作流，供智能体按需调用',
       0,
       'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM plugin_category WHERE category_code = 'skills'
);

INSERT INTO plugin_catalog (plugin_code, category, title, description, status, sort_order)
SELECT v.plugin_code, v.category, v.title, v.description, v.status, v.sort_order
FROM (
    SELECT 'skill-code-review' AS plugin_code,
           'skills' AS category,
           '代码审查 Skill' AS title,
           '按仓库规范审查变更：指出风险、给出可落地的修改建议，并控制评论语气。' AS description,
           'LISTED' AS status,
           10 AS sort_order
    UNION ALL
    SELECT 'skill-writing-brief',
           'skills',
           '文案撰写 Skill',
           '根据受众与渠道生成标题、正文与摘要，保持品牌语气一致。',
           'LISTED',
           20
    UNION ALL
    SELECT 'skill-data-report',
           'skills',
           '数据分析 Skill',
           '把表格与指标整理成结论先行的简报，并列出需要人工核对的假设。',
           'LISTED',
           30
) AS v
WHERE NOT EXISTS (
    SELECT 1 FROM plugin_catalog p WHERE p.plugin_code = v.plugin_code AND p.deleted = 0
);
