CREATE TABLE agent_sub_agent (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    agent_id BIGINT UNSIGNED NOT NULL,
    version_id BIGINT UNSIGNED NOT NULL,
    sub_agent_id BIGINT UNSIGNED NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_sub_agent (version_id, sub_agent_id),
    KEY idx_agent_sub_agent_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多 Agent 子智能体绑定';
