package com.boxai.agent.api;

import jakarta.validation.constraints.Size;

public record UpdateAgentConfigRequest(
        @Size(max = 16000) String configJson
) {
}
