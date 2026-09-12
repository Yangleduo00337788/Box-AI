package com.boxai.knowledge.api;

import java.util.List;

public record KnowledgeTestAnswerVO(
        String answer,
        List<KnowledgeSearchHitVO> citations
) {
}
