package com.boxai.agent.api;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAgentEmbedConfigRequest(
        @Size(max = 32) String themeColor,
        @Size(max = 512) String logoUrl,
        @Size(max = 500) String welcomeMessage,
        List<@Size(max = 200) String> suggestedQuestions
) {
}
