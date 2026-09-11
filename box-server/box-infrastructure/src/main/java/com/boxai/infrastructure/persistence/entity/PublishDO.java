package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("publish")
public class PublishDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("resource_type")
    private String resourceType;
    @Column("resource_id")
    private Long resourceId;
    @Column("version_id")
    private Long versionId;
    private String channel;
    private String status;
    @Column("published_by")
    private Long publishedBy;
    @Column("published_at")
    private LocalDateTime publishedAt;
    @Column("created_at")
    private LocalDateTime createdAt;
}
