package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record RecentAgentVO(
        Long id,
        String name,
        String status,
        LocalDateTime updatedAt
) {
}
