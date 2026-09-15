package com.boxai.workspace.api;

import java.time.LocalDateTime;

public record WorkspaceInvitationVO(
        Long id,
        String email,
        String roleCode,
        String token,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime createdAt
) {
}
