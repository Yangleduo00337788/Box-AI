package com.boxai.domain.conversation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Conversation {

    private Long id;
    private Long workspaceId;
    private Long agentId;
    private Long agentVersionId;
    private Long userId;
    private String title;
    private String status;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private String metadataJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
