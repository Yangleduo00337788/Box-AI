package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tenant")
public class TenantDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String name;
    private String slug;
    @Column("tenant_type")
    private String tenantType;
    @Column("plan_id")
    private Long planId;
    @Column("contact_email")
    private String contactEmail;
    private Integer status;
    @Column("owner_id")
    private Long ownerId;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
