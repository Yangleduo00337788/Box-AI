package com.boxai.agent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        @NotBlank @Size(max = 8000) String message,
        Boolean stream
) {
}
