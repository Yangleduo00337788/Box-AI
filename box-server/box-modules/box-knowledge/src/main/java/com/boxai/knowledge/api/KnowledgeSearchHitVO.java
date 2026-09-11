package com.boxai.knowledge.api;

public record KnowledgeSearchHitVO(
        Long chunkId,
        Long documentId,
        Integer chunkIndex,
        String content,
        Double score
) {
}
