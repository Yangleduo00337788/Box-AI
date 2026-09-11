CREATE TABLE plugin_category (
    category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
    label VARCHAR(64) NOT NULL COMMENT '展示名称',
    description VARCHAR(255) NULL COMMENT '分类描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件市场分类';

INSERT INTO plugin_category (category_code, label, description, sort_order) VALUES
('workflows', '工作流', '可视化编排自动化流程，连接模型、工具与业务系统', 10),
('knowledge', '知识库', '文档上传、切片与检索，为智能体提供 RAG 能力', 20),
('tools', '工具', 'HTTP、Function 等可调用工具，扩展 Agent 执行能力', 30),
('mcp', 'MCP', '连接 MCP 服务，统一管理外部能力接入', 40);

CREATE TABLE user_workspace_selection (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    workspace_id BIGINT UNSIGNED NOT NULL COMMENT '工作空间ID',
    selected_agent_id BIGINT UNSIGNED NULL COMMENT '当前选中智能体',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户工作空间选中状态';
