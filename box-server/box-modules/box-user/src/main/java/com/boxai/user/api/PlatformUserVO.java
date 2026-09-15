package com.boxai.user.api;

import java.time.LocalDateTime;

public record PlatformUserVO(
        Long id,
        String username,
        String email,
        String nickname,
        String userType,
        String platformAdminRole,
        Integer status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
