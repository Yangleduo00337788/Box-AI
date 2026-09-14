package com.boxai.agent.api.plugin;

public record UpdatePluginCatalogRequest(
        String category,
        String title,
        String description,
        String manifestJson,
        Integer sortOrder,
        String status
) {}
