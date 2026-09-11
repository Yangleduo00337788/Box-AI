package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("conversation")
public class ConversationDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("agent_id")
    private Long agentId;
    @Column("agent_version_id")
    private Long agentVersionId;
    @Column("user_id")
    private Long userId;
    private String title;
    private String status;
    @Column("message_count")
    private Integer messageCount;
    @Column("last_message_at")
    private LocalDateTime lastMessageAt;
    private String metadata;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
