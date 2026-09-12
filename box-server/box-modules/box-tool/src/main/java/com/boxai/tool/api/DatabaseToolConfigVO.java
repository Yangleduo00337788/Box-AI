package com.boxai.tool.api;

public record DatabaseToolConfigVO(
        String databaseType,
        String host,
        Integer port,
        String databaseName,
        String username,
        String allowedOperationsJson,
        Integer maxRows,
        Integer timeoutMs
) {
}
