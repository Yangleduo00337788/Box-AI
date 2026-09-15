package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record ConversationVO(
        Long id,
        Long agentId,
        String agentName,
        Long projectId,
        String title,
        String status,
        Integer messageCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
