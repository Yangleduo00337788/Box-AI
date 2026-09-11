package com.boxai.tenant.api;

public record AdminUserVO(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String userType
) {}
