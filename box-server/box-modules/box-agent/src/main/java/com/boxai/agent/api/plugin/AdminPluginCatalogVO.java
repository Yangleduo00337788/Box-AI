package com.boxai.agent.api.plugin;

public record AdminPluginCatalogVO(
        Long id,
        String pluginCode,
        String category,
        String title,
        String description,
        String manifestJson,
        String status,
        String reviewStatus,
        String visibility,
        String tenantIdsJson,
        Integer rolloutPercent,
        Integer sortOrder,
        Integer installCount
) {}
