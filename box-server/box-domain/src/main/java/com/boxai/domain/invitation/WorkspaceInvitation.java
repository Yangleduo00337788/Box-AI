package com.boxai.domain.invitation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkspaceInvitation {

    private Long id;
    private Long workspaceId;
    private Long tenantId;
    private String email;
    private String roleCode;
    private String token;
    private String status;
    private Long invitedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private Long acceptedUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
