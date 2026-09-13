package com.boxai.model.api.platform;

import jakarta.validation.constraints.Size;

public record UpdatePlatformModelRequest(
        @Size(max = 128) String modelName,
        @Size(max = 500) String description,
        Integer contextWindow,
        Integer maxOutputTokens,
        Integer sortOrder,
        Integer status
) {}
