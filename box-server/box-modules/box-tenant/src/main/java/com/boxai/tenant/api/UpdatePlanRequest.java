package com.boxai.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdatePlanRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        @NotNull BigDecimal priceMonthly,
        @NotNull Integer quotaAiCalls,
        @NotNull Long quotaTokens,
        @NotNull Integer quotaMembers,
        @NotNull Integer quotaWorkspaces,
        @NotNull Integer status
) {}
