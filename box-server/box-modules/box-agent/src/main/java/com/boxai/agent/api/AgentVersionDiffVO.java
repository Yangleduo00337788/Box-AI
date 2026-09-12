package com.boxai.agent.api;

public record AgentVersionDiffVO(
        String field,
        String label,
        String baseValue,
        String targetValue,
        boolean changed
) {}
