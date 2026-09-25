package com.boxai.agent.api.plugin;

public record PluginCatalogAssetVO(
        String storageKey,
        String fileName,
        long size,
        String contentType
) {
}
