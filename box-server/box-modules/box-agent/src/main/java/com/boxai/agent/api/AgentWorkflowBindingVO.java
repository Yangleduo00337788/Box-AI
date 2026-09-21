package com.boxai.agent.api;

public record AgentWorkflowBindingVO(
        Long id,
        Long workflowId,
        String workflowName,
        Boolean enabled,
        Boolean defaultWorkflow,
        Boolean callable
) {
}
