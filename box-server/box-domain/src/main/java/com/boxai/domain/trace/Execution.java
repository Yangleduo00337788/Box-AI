package com.boxai.domain.trace;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Execution {

    private Long id;
    private String executionNo;
    private String requestId;
    private Long workspaceId;
    private String executionType;
    private Long agentId;
    private Long agentVersionId;
    private Long workflowId;
    private Long workflowVersionId;
    private Long conversationId;
    private Long userId;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private Integer totalTokens;
    private LocalDateTime createdAt;
}
