package com.boxai.tool.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatabaseToolConfigRequest(
        @NotBlank @Size(max = 32) String databaseType,
        @NotBlank @Size(max = 255) String host,
        @Min(1) @Max(65535) Integer port,
        @NotBlank @Size(max = 128) String databaseName,
        @NotBlank @Size(max = 128) String username,
        @NotBlank String password,
        String allowedOperationsJson,
        @Min(1) @Max(1000) Integer maxRows,
        @Min(1000) @Max(120000) Integer timeoutMs
) {
}
