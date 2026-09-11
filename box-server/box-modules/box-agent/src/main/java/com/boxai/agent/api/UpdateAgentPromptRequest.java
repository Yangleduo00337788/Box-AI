package com.boxai.agent.api;

import jakarta.validation.constraints.Size;

public record UpdateAgentPromptRequest(
        @Size(max = 32000) String systemPrompt
) {
}
