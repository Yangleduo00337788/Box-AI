package com.boxai.agent.chat;

public record ResolvedAgentTool(
        Long toolId,
        String toolKey,
        String name,
        String description,
        String type,
        Long mcpServerId,
        String mcpToolName
) {
    public ResolvedAgentTool(Long toolId, String toolKey, String name, String description, String type) {
        this(toolId, toolKey, name, description, type, null, null);
    }
}
