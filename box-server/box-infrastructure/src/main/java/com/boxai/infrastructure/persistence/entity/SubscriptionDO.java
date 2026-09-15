package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("subscription")
public class SubscriptionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("tenant_id")
    private Long tenantId;
    @Column("plan_id")
    private Long planId;
    private String status;
    @Column("billing_cycle")
    private String billingCycle;
    @Column("current_period_start")
    private LocalDate currentPeriodStart;
    @Column("current_period_end")
    private LocalDate currentPeriodEnd;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
