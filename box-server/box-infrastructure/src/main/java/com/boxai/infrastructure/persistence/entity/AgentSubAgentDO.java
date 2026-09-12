package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("agent_sub_agent")
public class AgentSubAgentDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("agent_id")
    private Long agentId;
    @Column("version_id")
    private Long versionId;
    @Column("sub_agent_id")
    private Long subAgentId;
    private Integer enabled;
    @Column("sort_order")
    private Integer sortOrder;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
