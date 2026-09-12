package com.boxai.knowledge.application;

import com.boxai.knowledge.api.KnowledgeCitationVO;

import java.util.List;

public record KnowledgeRetrievalResult(
        String context,
        List<KnowledgeCitationVO> citations
) {
    public static KnowledgeRetrievalResult empty() {
        return new KnowledgeRetrievalResult(null, List.of());
    }
}
