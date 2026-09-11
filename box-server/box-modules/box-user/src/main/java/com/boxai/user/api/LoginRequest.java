package com.boxai.user.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String account,
        @NotBlank String password,
        @NotBlank String accountType
) {}
