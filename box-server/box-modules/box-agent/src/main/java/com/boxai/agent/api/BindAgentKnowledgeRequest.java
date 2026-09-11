package com.boxai.agent.api;

import jakarta.validation.constraints.NotNull;

public record BindAgentKnowledgeRequest(
        @NotNull Long knowledgeBaseId,
        Integer topK,
        String retrievalMode,
        Boolean rerankEnabled,
        Boolean citationEnabled
) {
}
