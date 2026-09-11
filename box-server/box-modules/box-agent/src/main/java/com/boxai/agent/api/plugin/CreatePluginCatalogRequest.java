package com.boxai.agent.api.plugin;

import jakarta.validation.constraints.NotBlank;

public record CreatePluginCatalogRequest(
        @NotBlank String pluginCode,
        @NotBlank String category,
        @NotBlank String title,
        String description,
        Integer sortOrder
) {}
