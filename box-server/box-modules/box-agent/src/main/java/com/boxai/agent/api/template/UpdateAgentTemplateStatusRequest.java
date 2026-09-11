package com.boxai.agent.api.template;

import jakarta.validation.constraints.NotBlank;

public record UpdateAgentTemplateStatusRequest(
        @NotBlank String status
) {
}
