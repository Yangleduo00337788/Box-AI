package com.boxai.tool.api;

public record FunctionToolConfigVO(
        String functionName,
        String functionCode,
        String runtime,
        Integer timeoutMs,
        Integer memoryLimitMb
) {
}
