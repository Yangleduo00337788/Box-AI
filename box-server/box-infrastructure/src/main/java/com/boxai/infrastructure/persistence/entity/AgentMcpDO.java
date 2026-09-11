package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("agent_mcp")
public class AgentMcpDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("agent_id")
    private Long agentId;
    @Column("version_id")
    private Long versionId;
    @Column("mcp_server_id")
    private Long mcpServerId;
    private Integer enabled;
    @Column("created_at")
    private LocalDateTime createdAt;
}
