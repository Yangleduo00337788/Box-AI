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
    private LocalDateTime createdAt;
}
