package com.boxai.model.api;

import jakarta.validation.constraints.NotBlank;

public record CreateProviderRequest(
        @NotBlank String providerCode,
        @NotBlank String providerName,
        String providerType,
        String baseUrl
) {}
