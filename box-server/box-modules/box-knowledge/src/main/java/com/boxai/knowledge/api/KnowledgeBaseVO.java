package com.boxai.knowledge.api;

import java.time.LocalDateTime;

public record KnowledgeBaseVO(
        Long id,
        String name,
        String description,
        String icon,
        Integer documentCount,
        Long chunkCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
