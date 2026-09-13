package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("embed_custom_domain")
public class EmbedCustomDomainDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("agent_id")
    private Long agentId;
    private String domain;
    @Column("verify_token")
    private String verifyToken;
    private Integer verified;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
