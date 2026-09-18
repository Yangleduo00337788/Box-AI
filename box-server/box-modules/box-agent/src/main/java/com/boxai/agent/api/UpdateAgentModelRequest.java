package com.boxai.agent.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateAgentModelRequest(
        @NotBlank String modelSource,
        Long platformModelId,
        Long modelId,
        String routingPreference,
        @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal topP,
        @Min(1) @Max(128000) Integer maxTokens,
        Boolean streamEnabled
) {}
