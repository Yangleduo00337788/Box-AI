package com.boxai.agent.api.plugin;

public record PluginCatalogVO(
        Long id,
        String pluginCode,
        String category,
        String title,
        String description,
        Integer installCount,
        Boolean installed,
        String resourceType,
        Long resourceId,
        String targetPath
) {}
