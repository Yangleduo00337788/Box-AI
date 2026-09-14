package com.boxai.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 128) String slug,
        @NotBlank String tenantType,
        @Email String contactEmail
) {}
