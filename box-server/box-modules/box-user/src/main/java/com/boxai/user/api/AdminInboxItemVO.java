package com.boxai.user.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AdminInboxItemVO(
        String key,
        String type,
        String title,
        String body,
        String linkUrl,
        String linkLabel,
        String theme,
        boolean dismissible,
        boolean read,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
) {
}
