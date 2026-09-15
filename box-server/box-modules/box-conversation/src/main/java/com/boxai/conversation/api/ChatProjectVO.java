package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record ChatProjectVO(
        Long id,
        String name,
        Integer conversationCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
