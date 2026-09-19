package com.boxai.knowledge.api;

import java.time.LocalDateTime;

public record KnowledgeDocumentVO(
        Long id,
        Long knowledgeBaseId,
        String name,
        String fileName,
        String fileType,
        Long fileSize,
        Integer chunkCount,
        String status,
        Integer progress,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
