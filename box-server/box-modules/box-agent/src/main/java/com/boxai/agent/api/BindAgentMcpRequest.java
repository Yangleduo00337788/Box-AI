package com.boxai.agent.api;

import jakarta.validation.constraints.NotNull;

public record BindAgentMcpRequest(
        @NotNull Long mcpServerId,
        Boolean enabled
) {
}
