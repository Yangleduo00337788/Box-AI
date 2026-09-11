package com.boxai.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddTenantMemberRequest(
        @NotBlank @Email String email,
        String roleCode
) {}
