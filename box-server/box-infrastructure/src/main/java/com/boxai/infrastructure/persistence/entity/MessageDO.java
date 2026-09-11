package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("message")
public class MessageDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("conversation_id")
    private Long conversationId;
    @Column("workspace_id")
    private Long workspaceId;
    private String role;
    private String content;
    @Column("content_type")
    private String contentType;
    @Column("sequence_no")
    private Integer sequenceNo;
    @Column("token_count")
    private Integer tokenCount;
    @Column("model_id")
    private Long modelId;
    private String metadata;
    @Column("created_at")
    private LocalDateTime createdAt;
}
