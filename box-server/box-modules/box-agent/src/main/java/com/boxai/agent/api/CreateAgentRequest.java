package com.boxai.agent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAgentRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String avatarUrl,
        Long platformModelId,
        Long modelId
) {
}
