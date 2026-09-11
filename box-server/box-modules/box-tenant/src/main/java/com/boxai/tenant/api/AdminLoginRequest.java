package com.boxai.tenant.api;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank String account,
        @NotBlank String password
) {}
