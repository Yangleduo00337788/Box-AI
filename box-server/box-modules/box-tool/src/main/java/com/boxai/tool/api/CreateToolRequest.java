package com.boxai.tool.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateToolRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 128) String toolKey,
        @Size(max = 512) String description,
        @NotBlank @Pattern(regexp = "HTTP|FUNCTION|DATABASE|CODE|MCP") String type,
        String inputSchemaJson,
        String outputSchemaJson,
        @Valid HttpToolConfigRequest httpConfig
) {
}
