package com.boxai.knowledge.api;

public record KnowledgeCitationVO(
        int index,
        Long chunkId,
        Long documentId,
        String documentName,
        Integer pageNumber,
        Integer chunkIndex,
        String content,
        Double score
) {
}
