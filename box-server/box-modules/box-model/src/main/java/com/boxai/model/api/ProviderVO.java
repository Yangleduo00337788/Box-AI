package com.boxai.model.api;

public record ProviderVO(
        Long id,
        String providerCode,
        String providerName,
        String providerType,
        String baseUrl,
        Integer status
) {}
