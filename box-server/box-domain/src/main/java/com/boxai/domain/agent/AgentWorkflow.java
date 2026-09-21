package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentWorkflow {

    private Long id;
    private Long agentId;
    private Long versionId;
    private Long workflowId;
    private Boolean enabled;
    private Boolean defaultWorkflow;
    private Boolean callable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
