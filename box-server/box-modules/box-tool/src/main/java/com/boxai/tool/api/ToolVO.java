package com.boxai.tool.api;

import java.time.LocalDateTime;

public record ToolVO(
        Long id,
        String name,
        String toolKey,
        String description,
        String type,
        String inputSchemaJson,
        String outputSchemaJson,
        Integer status,
        HttpToolConfigVO httpConfig,
        DatabaseToolConfigVO databaseConfig,
        FunctionToolConfigVO functionConfig,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
