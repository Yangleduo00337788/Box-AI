package com.boxai.trace.api;

import java.time.LocalDateTime;

public record ExecutionVO(
        Long id,
        String executionNo,
        String executionType,
        Long agentId,
        Long agentVersionId,
        Long conversationId,
        Long userId,
        String status,
        String inputJson,
        String outputJson,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs,
        Integer totalTokens,
        LocalDateTime createdAt
) {
}
