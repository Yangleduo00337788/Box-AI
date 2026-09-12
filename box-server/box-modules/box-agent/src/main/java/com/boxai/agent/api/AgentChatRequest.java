package com.boxai.agent.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgentChatRequest(
        @NotBlank @Size(max = 8000) String message,
        Boolean stream,
        @Valid List<AgentChatHistoryItem> history
) {
}
