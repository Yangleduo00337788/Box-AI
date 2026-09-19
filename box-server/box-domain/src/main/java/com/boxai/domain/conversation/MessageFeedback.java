package com.boxai.domain.conversation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageFeedback {
    private Long id;
    private Long workspaceId;
    private Long conversationId;
    private Long messageId;
    private Long userId;
    private String rating;
    private String content;
    /** PENDING | REPLIED，点踩待处理 */
    private String status;
    private String adminReply;
    private Long adminReplyBy;
    private LocalDateTime adminRepliedAt;
    private LocalDateTime createdAt;
}
