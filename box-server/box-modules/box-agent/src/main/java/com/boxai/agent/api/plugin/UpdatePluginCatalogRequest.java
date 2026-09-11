package com.boxai.agent.api.plugin;

public record UpdatePluginCatalogRequest(
        String category,
        String title,
        String description,
        Integer sortOrder,
        String status
) {}
