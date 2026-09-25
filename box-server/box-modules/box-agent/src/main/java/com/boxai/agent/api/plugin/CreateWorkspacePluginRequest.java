package com.boxai.agent.api.plugin;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspacePluginRequest(
        @NotBlank String category,
        @NotBlank String title,
        String description,
        @NotBlank String manifestJson
) {
}
