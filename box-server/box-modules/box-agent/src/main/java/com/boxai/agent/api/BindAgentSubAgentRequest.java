package com.boxai.agent.api;

import jakarta.validation.constraints.NotNull;

public record BindAgentSubAgentRequest(
        @NotNull Long subAgentId,
        Boolean enabled,
        Integer sortOrder
) {
}
