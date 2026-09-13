package com.boxai.agent.api;

public record PublishedEmbedResolveVO(
        Long agentId,
        String customDomain,
        AgentEmbedConfigVO embed
) {
}
