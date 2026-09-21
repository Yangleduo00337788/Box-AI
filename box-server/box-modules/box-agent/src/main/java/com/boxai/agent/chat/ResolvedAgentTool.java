package com.boxai.agent.chat;

public record ResolvedAgentTool(
        Long toolId,
        String toolKey,
        String name,
        String description,
        String type,
        Long mcpServerId,
        String mcpToolName,
        Long subAgentId,
        Long workflowId,
        boolean requireConfirmation
) {
    public ResolvedAgentTool(Long toolId, String toolKey, String name, String description, String type) {
        this(toolId, toolKey, name, description, type, null, null, null, null, false);
    }

    public ResolvedAgentTool(Long toolId,
                             String toolKey,
                             String name,
                             String description,
                             String type,
                             Long mcpServerId,
                             String mcpToolName) {
        this(toolId, toolKey, name, description, type, mcpServerId, mcpToolName, null, null, false);
    }

    public ResolvedAgentTool(Long toolId,
                             String toolKey,
                             String name,
                             String description,
                             String type,
                             Long mcpServerId,
                             String mcpToolName,
                             Long subAgentId) {
        this(toolId, toolKey, name, description, type, mcpServerId, mcpToolName, subAgentId, null, false);
    }
}
