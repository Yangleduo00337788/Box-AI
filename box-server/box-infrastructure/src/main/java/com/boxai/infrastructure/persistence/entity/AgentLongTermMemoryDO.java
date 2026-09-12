package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("agent_long_term_memory")
public class AgentLongTermMemoryDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("agent_id")
    private Long agentId;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("user_id")
    private Long userId;
    private String content;
    @Column("es_document_id")
    private String esDocumentId;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
