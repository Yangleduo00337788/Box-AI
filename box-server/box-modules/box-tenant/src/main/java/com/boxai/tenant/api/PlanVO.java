package com.boxai.tenant.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanVO(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal priceMonthly,
        Integer quotaAiCalls,
        Long quotaTokens,
        Integer quotaMembers,
        Integer quotaWorkspaces,
        Integer status,
        LocalDateTime createdAt
) {}
