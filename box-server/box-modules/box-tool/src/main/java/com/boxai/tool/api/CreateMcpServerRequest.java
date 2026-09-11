package com.boxai.tool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateMcpServerRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{2,64}") String serverKey,
        String description,
        @NotBlank String endpointUrl,
        String transportType,
        String authType,
        String authConfigJson
) {
}
