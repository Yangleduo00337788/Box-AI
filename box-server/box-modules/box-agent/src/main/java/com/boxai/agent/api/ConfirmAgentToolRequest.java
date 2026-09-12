package com.boxai.agent.api;

import jakarta.validation.constraints.NotBlank;

public record ConfirmAgentToolRequest(
        @NotBlank String confirmationToken
) {
}
