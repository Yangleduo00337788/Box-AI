package com.boxai.user.api;

import java.time.LocalDateTime;

public record AuditLogVO(
        Long id,
        Long workspaceId,
        Long userId,
        String action,
        String resourceType,
        String resourceId,
        String resourceName,
        String result,
        String ipAddress,
        String traceId,
        String detail,
        LocalDateTime createdAt
) {
}
