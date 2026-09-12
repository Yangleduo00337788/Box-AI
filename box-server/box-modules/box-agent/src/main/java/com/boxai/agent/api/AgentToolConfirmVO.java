package com.boxai.agent.api;

public record AgentToolConfirmVO(
        String toolKey,
        String toolName,
        String output
) {
}
