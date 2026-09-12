package com.boxai.tool.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateToolRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        @NotBlank @Pattern(regexp = "HTTP|FUNCTION|DATABASE|CODE|MCP") String type,
        String inputSchemaJson,
        String outputSchemaJson,
        Integer status,
        @Valid HttpToolConfigRequest httpConfig,
        @Valid DatabaseToolConfigRequest databaseConfig,
        @Valid FunctionToolConfigRequest functionConfig
) {
}
