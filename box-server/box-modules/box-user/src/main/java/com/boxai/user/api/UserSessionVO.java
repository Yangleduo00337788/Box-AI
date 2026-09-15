package com.boxai.user.api;

import java.time.LocalDateTime;

public record UserSessionVO(
        String sessionId,
        String deviceName,
        String ipAddress,
        LocalDateTime lastActiveAt,
        LocalDateTime expiresAt,
        boolean current
) {
}
