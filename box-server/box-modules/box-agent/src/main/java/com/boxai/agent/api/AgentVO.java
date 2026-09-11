package com.boxai.agent.api;

import java.time.LocalDateTime;

public record AgentVO(
        Long id,
        String name,
        String description,
        String avatarUrl,
        String status,
        Integer draftVersion,
        Integer publishedVersion,
        String modelSource,
        Long modelId,
        String modelName,
        Long platformModelId,
        String platformModelName,
        String systemPrompt,
        java.math.BigDecimal temperature,
        java.math.BigDecimal topP,
        Integer maxTokens,
        Boolean streamEnabled,
        Boolean memoryEnabled,
        Integer memoryWindowSize,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
