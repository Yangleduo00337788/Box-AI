package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentSubAgent {

    private Long id;
    private Long agentId;
    private Long versionId;
    private Long subAgentId;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
