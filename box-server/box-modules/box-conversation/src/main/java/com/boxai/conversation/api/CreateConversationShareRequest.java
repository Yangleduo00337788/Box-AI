package com.boxai.conversation.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateConversationShareRequest(
        String title,
        @NotEmpty List<Long> messageIds
) {
}
