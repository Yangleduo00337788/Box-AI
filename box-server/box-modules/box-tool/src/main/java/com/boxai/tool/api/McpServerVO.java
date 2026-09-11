package com.boxai.tool.api;

import java.time.LocalDateTime;

public record McpServerVO(
        Long id,
        String name,
        String serverKey,
        String description,
        String transportType,
        String endpointUrl,
        String authType,
        String toolCatalogJson,
        Integer status,
        LocalDateTime lastSyncAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
