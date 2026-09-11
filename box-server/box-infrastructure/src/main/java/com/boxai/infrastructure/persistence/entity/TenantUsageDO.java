package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tenant_usage")
public class TenantUsageDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tenant_id")
    private Long tenantId;
    private String period;
    @Column("ai_calls")
    private Integer aiCalls;
    private Long tokens;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
