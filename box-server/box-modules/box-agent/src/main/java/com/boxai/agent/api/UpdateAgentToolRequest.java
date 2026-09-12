package com.boxai.agent.api;

public record UpdateAgentToolRequest(
        Boolean enabled,
        Boolean requireConfirmation
) {
}
