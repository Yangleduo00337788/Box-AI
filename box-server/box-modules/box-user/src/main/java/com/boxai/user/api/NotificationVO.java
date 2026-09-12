package com.boxai.user.api;

import java.time.LocalDateTime;

public record NotificationVO(
        Long id,
        String title,
        String content,
        String category,
        String linkUrl,
        Boolean read,
        LocalDateTime createdAt
) {
}
