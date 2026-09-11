package com.boxai.tool.api;

public record ToolTestResultVO(
        int statusCode,
        String body,
        long durationMs
) {
}
