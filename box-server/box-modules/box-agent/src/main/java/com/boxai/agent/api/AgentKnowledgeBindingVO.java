package com.boxai.agent.api;

public record AgentKnowledgeBindingVO(
        Long id,
        Long knowledgeBaseId,
        Integer topK,
        String retrievalMode,
        Boolean rerankEnabled,
        Boolean citationEnabled
) {
}
