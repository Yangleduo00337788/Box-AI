package com.boxai.tool.api;

public record HttpToolConfigVO(
        String method,
        String url,
        String headersJson,
        String queryParamsJson,
        String bodyType,
        String bodyTemplate,
        Integer timeoutMs,
        Boolean allowRedirect
) {
}
