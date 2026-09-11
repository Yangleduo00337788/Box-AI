package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentTool {

    private Long id;
    private Long agentId;
    private Long versionId;
    private Long toolId;
    private Boolean enabled;
    private Boolean requireConfirmation;
    private String configJson;
    private LocalDateTime createdAt;
}
