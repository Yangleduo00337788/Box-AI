package com.boxai.agent.api;

import com.boxai.knowledge.api.KnowledgeCitationVO;

import java.util.List;

public record AgentChatVO(
        String content,
        List<KnowledgeCitationVO> citations,
        Long executionId
) {
    public AgentChatVO(String content) {
        this(content, List.of(), null);
    }
}
