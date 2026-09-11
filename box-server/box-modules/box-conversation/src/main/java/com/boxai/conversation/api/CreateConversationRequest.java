package com.boxai.conversation.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotNull Long agentId,
        @Size(max = 255) String title
) {
}
