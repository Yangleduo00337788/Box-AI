package com.boxai.agent.api.plugin;

public record AdminPluginCatalogVO(
        Long id,
        String pluginCode,
        String category,
        String title,
        String description,
        String status,
        Integer sortOrder,
        Integer installCount
) {}
