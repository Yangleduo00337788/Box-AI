package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workflow")
public class WorkflowDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    private String name;
    private String description;
    private String status;
    @Column("draft_version_id")
    private Long draftVersionId;
    @Column("published_version_id")
    private Long publishedVersionId;
    @Column("webhook_token")
    private String webhookToken;
    @Column("webhook_secret")
    private String webhookSecret;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
