package com.boxai.agent.chat;

import java.util.List;

public record ConversationPluginRound(
        List<ResolvedAgentTool> extraTools,
        String skillPromptBlock
) {
    public static ConversationPluginRound empty() {
        return new ConversationPluginRound(List.of(), null);
    }
}
