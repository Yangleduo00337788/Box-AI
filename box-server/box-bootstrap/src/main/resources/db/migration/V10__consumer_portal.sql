ALTER TABLE sys_user
    ADD COLUMN bio VARCHAR(255) NULL COMMENT '个人简介' AFTER avatar_url;

CREATE TABLE user_preference (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    theme VARCHAR(16) NOT NULL DEFAULT 'light' COMMENT 'light/dark/system',
    send_with_enter TINYINT NOT NULL DEFAULT 1 COMMENT '1 Enter发送 0 Ctrl+Enter发送',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好';

CREATE TABLE user_sidebar_pin (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    workspace_id BIGINT UNSIGNED NOT NULL,
    pin_type VARCHAR(16) NOT NULL COMMENT 'AGENT|CONVERSATION',
    target_id BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_workspace_pin (user_id, workspace_id, pin_type, target_id),
    KEY idx_user_workspace (user_id, workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='侧边栏固定项';

CREATE TABLE plugin_catalog (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plugin_code VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL COMMENT 'workflows|knowledge|tools|mcp',
    title VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'LISTED',
    sort_order INT NOT NULL DEFAULT 0,
    install_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plugin_code (plugin_code),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件市场目录';

CREATE TABLE workspace_plugin_install (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT UNSIGNED NOT NULL,
    plugin_id BIGINT UNSIGNED NOT NULL,
    installed_by BIGINT UNSIGNED NOT NULL,
    installed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_plugin (workspace_id, plugin_id),
    KEY idx_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作空间插件安装';

CREATE TABLE system_config (
    config_key VARCHAR(128) NOT NULL,
    config_value LONGTEXT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

INSERT INTO plugin_catalog (plugin_code, category, title, description, status, sort_order) VALUES
('wf-1', 'workflows', '客服自动分流', '根据意图将用户请求路由到不同处理流程。', 'LISTED', 10),
('wf-2', 'workflows', '日报生成流水线', '定时汇总数据并生成结构化业务日报。', 'LISTED', 20),
('wf-3', 'workflows', '审批协同流程', '串联表单、通知与人工确认节点。', 'LISTED', 30),
('kb-1', 'knowledge', '产品文档库', '导入手册与 FAQ，支持语义检索问答。', 'LISTED', 10),
('kb-2', 'knowledge', '代码规范库', '沉淀团队编码规范，供审查助手引用。', 'LISTED', 20),
('kb-3', 'knowledge', '销售话术库', '按场景组织话术模板与成功案例。', 'LISTED', 30),
('tool-1', 'tools', 'HTTP 请求工具', '调用 REST API 并解析响应给 Agent 使用。', 'LISTED', 10),
('tool-2', 'tools', '表格解析工具', '读取 CSV / Excel 并输出结构化结果。', 'LISTED', 20),
('tool-3', 'tools', '代码执行沙箱', '在安全环境中运行脚本并返回输出。', 'LISTED', 30),
('mcp-1', 'mcp', 'GitHub MCP', '连接仓库、Issue 与 Pull Request 操作。', 'LISTED', 10),
('mcp-2', 'mcp', '数据库 MCP', '通过 MCP 协议查询与写入业务数据。', 'LISTED', 20),
('mcp-3', 'mcp', '浏览器 MCP', '让 Agent 以 MCP 方式驱动网页自动化。', 'LISTED', 30);

INSERT INTO system_config (config_key, config_value, description) VALUES
('support.email', 'support@box.ai', '客服邮箱'),
('about.slogan', '把模型、知识、工具、工作流装进一个 Box，让任何人都能构建自己的 AI Agent。', '产品 Slogan'),
('about.product_name', 'Box AI', '产品名称'),
('about.positioning', 'AI Agent & Workflow Platform', '产品定位'),
('app.version', '1.0.0', '客户端版本'),
('legal.privacy', '["Box 会收集账号信息、工作空间数据与对话内容，用于提供 AI Agent 创建、编排与运行服务。","我们不会向无关第三方出售你的个人数据。模型调用相关的提示词与回复会按套餐策略保留，用于排障与计费统计。","你可以在设置中查看账号信息，如需删除账号或导出数据，请联系企业管理员或 support@box.ai。"]', '隐私政策段落'),
('legal.terms', '["使用 Box 即表示你同意遵守适用的法律法规，不得利用平台生成违法、侵权或有害内容。","企业管理员对工作空间成员权限、模型密钥与数据合规负有管理责任。","平台可能根据产品迭代调整功能与配额策略，重大变更将通过站内通知或邮件告知。"]', '服务协议段落'),
('legal.updated_at', '2026-09-11', '法律文档最后更新');
