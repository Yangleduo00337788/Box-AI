package com.boxai.agent.api.plugin;

public record UpdatePluginCategoryRequest(
        String label,
        String description,
        Integer sortOrder,
        String status
) {}
