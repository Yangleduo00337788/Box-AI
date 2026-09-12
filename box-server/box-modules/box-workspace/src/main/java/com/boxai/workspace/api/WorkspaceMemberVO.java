package com.boxai.workspace.api;

import java.time.LocalDateTime;

public record WorkspaceMemberVO(
        Long id,
        Long userId,
        String email,
        String nickname,
        String roleCode,
        Integer status,
        LocalDateTime joinedAt
) {
}
