package com.boxai.model.api;

public record ModelVO(
        Long id,
        Long providerId,
        String providerName,
        String modelCode,
        String modelName,
        String modelType,
        Boolean supportStreaming,
        Boolean supportToolCalling,
        Integer status
) {}
