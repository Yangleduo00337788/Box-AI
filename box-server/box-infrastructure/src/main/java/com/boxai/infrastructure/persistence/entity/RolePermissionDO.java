package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("sys_role_permission")
public class RolePermissionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("role_id")
    private Long roleId;
    @Column("permission_id")
    private Long permissionId;
    @Column("created_at")
    private LocalDateTime createdAt;
}
