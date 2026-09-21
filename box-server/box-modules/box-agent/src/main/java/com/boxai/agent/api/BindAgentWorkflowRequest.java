package com.boxai.agent.api;

import jakarta.validation.constraints.NotNull;

public record BindAgentWorkflowRequest(
        @NotNull Long workflowId,
        Boolean enabled,
        Boolean defaultWorkflow,
        Boolean callable
) {
}
