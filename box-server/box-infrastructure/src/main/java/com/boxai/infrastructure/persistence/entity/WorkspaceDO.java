package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workspace")
public class WorkspaceDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tenant_id")
    private Long tenantId;
    private String name;
    private String slug;
    private String description;
    @Column("avatar_url")
    private String avatarUrl;
    @Column("owner_id")
    private Long ownerId;
    private Integer status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("created_by")
    private Long createdBy;
    @Column("updated_by")
    private Long updatedBy;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
