package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record RecentConversationVO(
        Long id,
        Long agentId,
        String title,
        LocalDateTime lastMessageAt,
        LocalDateTime updatedAt
) {
}
