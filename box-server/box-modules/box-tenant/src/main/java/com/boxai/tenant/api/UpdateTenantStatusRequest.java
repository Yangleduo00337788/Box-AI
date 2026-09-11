package com.boxai.tenant.api;

import jakarta.validation.constraints.NotNull;

public record UpdateTenantStatusRequest(
        @NotNull Integer status
) {}
