package com.boxai.agent.api.market;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateMarketRolloutRequest(
        @NotBlank String visibility,
        String tenantIds,
        @Min(0) @Max(100) Integer rolloutPercent
) {
}
