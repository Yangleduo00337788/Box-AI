package com.boxai.domain.conversation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationShare {
    private Long id;
    private String token;
    private Long workspaceId;
    private Long conversationId;
    private String title;
    private String payloadJson;
    private Long createdBy;
    private LocalDateTime createdAt;
}
