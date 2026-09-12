package com.boxai.trace.api;

import java.time.LocalDateTime;

public record TraceSpanVO(
        String spanId,
        String parentSpanId,
        String spanType,
        String name,
        String status,
        String inputJson,
        String outputJson,
        Long durationMs,
        String errorMessage,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
