package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("agent_tool")
public class AgentToolDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("agent_id")
    private Long agentId;
    @Column("version_id")
    private Long versionId;
    @Column("tool_id")
    private Long toolId;
    private Integer enabled;
    @Column("require_confirmation")
    private Integer requireConfirmation;
    private String config;
    @Column("created_at")
    private LocalDateTime createdAt;
}
