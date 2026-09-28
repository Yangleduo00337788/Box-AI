package com.boxai.user.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record NotificationVO(
        Long id,
        String key,
        String title,
        String content,
        String category,
        String linkUrl,
        String linkLabel,
        Boolean dismissible,
        @JsonProperty("read")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Boolean read,
        LocalDateTime createdAt
) {
}
