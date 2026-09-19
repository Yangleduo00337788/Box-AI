package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("message_feedback")
public class MessageFeedbackDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("conversation_id")
    private Long conversationId;
    @Column("message_id")
    private Long messageId;
    @Column("user_id")
    private Long userId;
    private String rating;
    private String content;
    private String status;
    @Column("admin_reply")
    private String adminReply;
    @Column("admin_reply_by")
    private Long adminReplyBy;
    @Column("admin_replied_at")
    private LocalDateTime adminRepliedAt;
    @Column("created_at")
    private LocalDateTime createdAt;
}
