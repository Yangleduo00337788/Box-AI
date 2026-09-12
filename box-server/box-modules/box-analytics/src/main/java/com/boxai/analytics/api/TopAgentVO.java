package com.boxai.analytics.api;

public record TopAgentVO(
        Long agentId,
        String agentName,
        int executionCount,
        double successRate
) {
}
