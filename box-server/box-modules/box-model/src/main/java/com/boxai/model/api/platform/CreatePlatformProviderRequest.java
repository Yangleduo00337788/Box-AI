package com.boxai.model.api.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlatformProviderRequest(
        @NotBlank @Size(max = 64) String providerCode,
        @NotBlank @Size(max = 128) String providerName,
        String providerType,
        String baseUrl
) {}
