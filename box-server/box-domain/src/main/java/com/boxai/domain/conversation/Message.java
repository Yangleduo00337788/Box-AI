package com.boxai.domain.conversation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Message {

    private Long id;
    private Long conversationId;
    private Long workspaceId;
    private String role;
    private String content;
    private String contentType;
    private Integer sequenceNo;
    private Integer tokenCount;
    private Long modelId;
    private String metadataJson;
    private LocalDateTime createdAt;
}
