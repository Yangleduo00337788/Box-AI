package com.boxai.agent.api.plugin;

import jakarta.validation.constraints.NotBlank;

public record UpdatePluginReviewRequest(
        @NotBlank String reviewStatus
) {
}
