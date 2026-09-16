package com.boxai.agent.api.template;

import jakarta.validation.constraints.NotBlank;

public record UpdateAgentTemplateReviewRequest(
        @NotBlank String reviewStatus
) {
}
