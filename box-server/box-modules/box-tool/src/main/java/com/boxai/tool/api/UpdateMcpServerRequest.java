package com.boxai.tool.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateMcpServerRequest(
        @NotBlank String name,
        String description,
        @NotBlank String endpointUrl,
        String transportType,
        String authType,
        String authConfigJson,
        Integer status
) {
}
