package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("sys_permission")
public class PermissionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("permission_code")
    private String permissionCode;
    @Column("permission_name")
    private String permissionName;
    @Column("resource_type")
    private String resourceType;
    private String action;
    private String description;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
