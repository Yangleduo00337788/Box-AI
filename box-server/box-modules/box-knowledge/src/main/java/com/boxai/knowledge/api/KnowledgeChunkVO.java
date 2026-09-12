package com.boxai.knowledge.api;

public record KnowledgeChunkVO(
        Long id,
        Integer chunkIndex,
        String content,
        Integer tokenCount,
        String status
) {}
