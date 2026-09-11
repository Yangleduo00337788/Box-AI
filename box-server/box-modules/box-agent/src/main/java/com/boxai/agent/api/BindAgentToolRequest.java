package com.boxai.agent.api;

import jakarta.validation.constraints.NotNull;

public record BindAgentToolRequest(
        @NotNull Long toolId,
        Boolean enabled,
        Boolean requireConfirmation,
        String configJson
) {
}
