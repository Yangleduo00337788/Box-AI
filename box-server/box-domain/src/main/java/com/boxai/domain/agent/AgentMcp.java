package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentMcp {

    private Long id;
    private Long agentId;
    private Long versionId;
    private Long mcpServerId;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
