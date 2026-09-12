package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("plan")
public class PlanDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String code;
    private String name;
    private String description;
    @Column("price_monthly")
    private BigDecimal priceMonthly;
    @Column("quota_ai_calls")
    private Integer quotaAiCalls;
    @Column("quota_tokens")
    private Long quotaTokens;
    @Column("quota_members")
    private Integer quotaMembers;
    @Column("quota_workspaces")
    private Integer quotaWorkspaces;
    @Column("quota_knowledge_bases")
    private Integer quotaKnowledgeBases;
    @Column("byok_enabled")
    private Integer byokEnabled;
    private Integer status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
