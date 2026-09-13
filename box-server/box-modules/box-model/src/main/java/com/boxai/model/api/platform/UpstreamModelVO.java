package com.boxai.model.api.platform;

public record UpstreamModelVO(
        String modelCode,
        String modelName,
        boolean imported,
        Integer contextWindow,
        Integer maxOutputTokens
) {}
