package com.boxai.agent.api;

import java.time.LocalDateTime;

public record AgentLongTermMemoryVO(
        Long id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
