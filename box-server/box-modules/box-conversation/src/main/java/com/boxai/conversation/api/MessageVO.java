package com.boxai.conversation.api;

import java.time.LocalDateTime;

public record MessageVO(
        Long id,
        String role,
        String content,
        String contentType,
        Integer sequenceNo,
        LocalDateTime createdAt
) {
}
