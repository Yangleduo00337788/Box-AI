package com.boxai.model.api.platform;

public record PlatformOcrDefaultVO(
        Long configuredPlatformModelId,
        Long platformModelId,
        String modelCode,
        String modelName,
        String providerName,
        boolean runnable,
        String resolutionMode,
        String resolutionHint
) {
}
