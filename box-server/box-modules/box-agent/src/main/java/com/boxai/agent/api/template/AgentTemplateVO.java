package com.boxai.agent.api.template;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgentTemplateVO(
        Long id,
        String templateCode,
        String name,
        String description,
        String avatarUrl,
        String category,
        String systemPrompt,
        Long platformModelId,
        String platformModelName,
        BigDecimal temperature,
        BigDecimal topP,
        Integer maxTokens,
        Boolean streamEnabled,
        String status,
        String reviewStatus,
        Integer sortOrder,
        Integer installCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
