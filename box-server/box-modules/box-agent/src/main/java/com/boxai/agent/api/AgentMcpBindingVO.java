package com.boxai.agent.api;

public record AgentMcpBindingVO(
        Long id,
        Long mcpServerId,
        String mcpServerName,
        String serverKey,
        Boolean enabled,
        String toolCatalogJson
) {
}
