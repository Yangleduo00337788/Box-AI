package com.boxai.tenant.api;

import jakarta.validation.constraints.NotNull;

public record UpdateTenantMemberStatusRequest(
        @NotNull Integer status
) {}
