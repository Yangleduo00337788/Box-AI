package com.boxai.tool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HttpToolConfigRequest(
        @NotBlank @Size(max = 16) String method,
        @NotBlank @Size(max = 2048) String url,
        String headersJson,
        String queryParamsJson,
        String bodyType,
        String bodyTemplate,
        Integer timeoutMs,
        Boolean allowRedirect
) {
}
