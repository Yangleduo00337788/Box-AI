package com.boxai.agent.chat;

public record ResolvedAgentTool(
        Long toolId,
        String toolKey,
        String name,
        String description,
        String type,
        Long mcpServerId,
        String mcpToolName,
        Long subAgentId
) {
    public ResolvedAgentTool(Long toolId, String toolKey, String name, String description, String type) {
        this(toolId, toolKey, name, description, type, null, null, null);
    }

    public ResolvedAgentTool(Long toolId,
                             String toolKey,
                             String name,
                             String description,
                             String type,
                             Long mcpServerId,
                             String mcpToolName) {
        this(toolId, toolKey, name, description, type, mcpServerId, mcpToolName, null);
    }
}
