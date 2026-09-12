package com.boxai.agent.api;

import java.util.List;

public record AgentEmbedConfigVO(
        String themeColor,
        String logoUrl,
        String welcomeMessage,
        List<String> suggestedQuestions,
        String agentName
) {
}
