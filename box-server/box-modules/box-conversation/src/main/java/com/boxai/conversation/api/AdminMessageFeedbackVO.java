package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record AdminMessageFeedbackVO(
        Long id,
        Long workspaceId,
        Long conversationId,
        Long messageId,
        Long userId,
        String userEmail,
        String userNickname,
        String rating,
        String content,
        String status,
        String messageExcerpt,
        String adminReply,
        Long adminReplyBy,
        LocalDateTime adminRepliedAt,
        LocalDateTime createdAt
) {
}
