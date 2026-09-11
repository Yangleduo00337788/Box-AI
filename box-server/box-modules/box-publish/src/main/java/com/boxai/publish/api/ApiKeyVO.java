package com.boxai.publish.api;

import java.time.LocalDateTime;

public record ApiKeyVO(
        Long id,
        String name,
        String keyPrefix,
        Integer status,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
}
