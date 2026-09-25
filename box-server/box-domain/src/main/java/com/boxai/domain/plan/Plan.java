package com.boxai.domain.plan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Plan {

    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal priceMonthly;
    private Integer quotaAiCalls;
    private Long quotaTokens;
    private Integer quotaMembers;
    private Integer quotaWorkspaces;
    private Integer quotaKnowledgeBases;
    private String audience;
    private String overagePolicy;
    private Integer byokEnabled;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
