package com.boxai.agent.api.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAgentTemplateRequest(
        @NotBlank @Size(max = 64) String templateCode,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String avatarUrl,
        @Size(max = 64) String category,
        String systemPrompt,
        @NotNull Long platformModelId,
        BigDecimal temperature,
        BigDecimal topP,
        Integer maxTokens,
        Boolean streamEnabled,
        Integer sortOrder
) {
}
