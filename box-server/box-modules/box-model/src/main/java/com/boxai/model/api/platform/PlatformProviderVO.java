package com.boxai.model.api.platform;

public record PlatformProviderVO(
        Long id,
        String providerCode,
        String providerName,
        String providerType,
        String baseUrl,
        Integer status
) {}
