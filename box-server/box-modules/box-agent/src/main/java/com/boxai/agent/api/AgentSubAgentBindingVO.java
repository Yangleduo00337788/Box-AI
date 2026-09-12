package com.boxai.agent.api;

public record AgentSubAgentBindingVO(
        Long id,
        Long subAgentId,
        String subAgentName,
        Boolean enabled,
        Integer sortOrder
) {
}
