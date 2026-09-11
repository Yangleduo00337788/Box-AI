package com.boxai.agent.api.plugin;

import jakarta.validation.constraints.NotBlank;

public record CreatePluginCategoryRequest(
        @NotBlank String categoryCode,
        @NotBlank String label,
        String description,
        Integer sortOrder
) {}
