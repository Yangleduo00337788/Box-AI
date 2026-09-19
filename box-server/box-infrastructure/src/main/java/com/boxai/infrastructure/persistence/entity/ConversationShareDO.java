package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("conversation_share")
public class ConversationShareDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String token;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("conversation_id")
    private Long conversationId;
    private String title;
    @Column("payload_json")
    private String payloadJson;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
}
