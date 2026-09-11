package com.boxai.model.api.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlatformModelRequest(
        @NotNull Long providerId,
        @NotBlank @Size(max = 128) String modelCode,
        @NotBlank @Size(max = 128) String modelName,
        @Size(max = 500) String description,
        Integer contextWindow,
        Integer maxOutputTokens,
        Integer sortOrder
) {}
