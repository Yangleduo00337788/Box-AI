package com.boxai.agent.api;

public record AgentToolBindingVO(
        Long id,
        Long toolId,
        Boolean enabled,
        Boolean requireConfirmation,
        String configJson
) {
}
