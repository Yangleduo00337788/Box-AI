package com.boxai.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionEmployeeRequest(
        @NotBlank @Size(min = 2, max = 64) String account,
        @NotBlank @Size(min = 8, max = 64) String password,
        String nickname,
        String email,
        String roleCode
) {}
