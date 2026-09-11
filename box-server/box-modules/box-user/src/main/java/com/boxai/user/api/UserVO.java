package com.boxai.user.api;

public record UserVO(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String bio,
        String userType
) {}
