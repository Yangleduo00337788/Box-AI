package com.boxai.agent.chat;

public record ResolvedAgentTool(
        Long toolId,
        String toolKey,
        String name,
        String description,
        String type
) {
}
