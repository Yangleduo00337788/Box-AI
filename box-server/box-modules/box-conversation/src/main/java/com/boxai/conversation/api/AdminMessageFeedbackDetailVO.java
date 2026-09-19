package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record AdminMessageFeedbackDetailVO(
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
        String messageContent,
        String adminReply,
        Long adminReplyBy,
        LocalDateTime adminRepliedAt,
        LocalDateTime createdAt
) {
}
