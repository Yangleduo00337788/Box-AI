package com.boxai.conversation.api;

public record TopAgentVO(
        Long agentId,
        String agentName,
        int executionCount,
        double successRate
) {
}
