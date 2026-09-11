package com.boxai.model.api.platform;

public record PlatformModelVO(
        Long id,
        Long providerId,
        String providerName,
        String modelCode,
        String modelName,
        String description,
        Integer contextWindow,
        Integer maxOutputTokens,
        Boolean supportStreaming,
        Integer status
) {}
