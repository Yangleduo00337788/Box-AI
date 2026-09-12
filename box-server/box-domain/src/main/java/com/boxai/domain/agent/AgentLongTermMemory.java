package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentLongTermMemory {

    private Long id;
    private Long agentId;
    private Long workspaceId;
    private Long userId;
    private String content;
    private String esDocumentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
