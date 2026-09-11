package com.boxai.model.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateModelRequest(
        @NotNull Long providerId,
        @NotBlank String modelCode,
        @NotBlank String modelName,
        String modelType,
        Boolean supportStreaming,
        Boolean supportToolCalling,
        Integer contextWindow,
        Integer maxOutputTokens
) {}
