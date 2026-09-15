package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workspace_invitation")
public class WorkspaceInvitationDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("tenant_id")
    private Long tenantId;
    private String email;
    @Column("role_code")
    private String roleCode;
    private String token;
    private String status;
    @Column("invited_by")
    private Long invitedBy;
    @Column("expires_at")
    private LocalDateTime expiresAt;
    @Column("accepted_at")
    private LocalDateTime acceptedAt;
    @Column("accepted_user_id")
    private Long acceptedUserId;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
