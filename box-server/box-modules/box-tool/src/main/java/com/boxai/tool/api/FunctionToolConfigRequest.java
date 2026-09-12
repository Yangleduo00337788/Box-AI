package com.boxai.tool.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FunctionToolConfigRequest(
        @NotBlank @Size(max = 128) String functionName,
        @NotBlank String functionCode,
        @NotBlank @Size(max = 32) String runtime,
        @Min(1000) @Max(120000) Integer timeoutMs,
        @Min(16) @Max(512) Integer memoryLimitMb
) {
}
