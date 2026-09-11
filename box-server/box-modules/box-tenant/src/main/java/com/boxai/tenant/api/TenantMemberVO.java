package com.boxai.tenant.api;

import java.time.LocalDateTime;

public record TenantMemberVO(
        Long id,
        Long userId,
        String email,
        String nickname,
        String roleCode,
        Integer status,
        LocalDateTime joinedAt
) {}
