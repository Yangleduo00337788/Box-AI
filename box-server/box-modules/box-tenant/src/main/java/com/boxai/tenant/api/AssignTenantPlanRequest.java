package com.boxai.tenant.api;

import jakarta.validation.constraints.NotNull;

public record AssignTenantPlanRequest(
        @NotNull Long planId
) {}
