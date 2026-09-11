package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_workspace_selection")
public class UserWorkspaceSelectionDO {

    @Id
    @Column("user_id")
    private Long userId;
    @Id
    @Column("workspace_id")
    private Long workspaceId;
    @Column("selected_agent_id")
    private Long selectedAgentId;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
