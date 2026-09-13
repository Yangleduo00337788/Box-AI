package com.boxai.model.api.platform;

import jakarta.validation.constraints.Size;

public record UpdatePlatformProviderRequest(
        @Size(max = 128) String providerName,
        String providerType,
        String baseUrl,
        Integer status
) {}
