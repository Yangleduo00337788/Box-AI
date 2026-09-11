package com.boxai.runtime.api;

import java.util.Map;

public record WorkflowNodeTraceVO(
        String nodeId,
        String nodeType,
        String status,
        long durationMs,
        Map<String, Object> output,
        String errorMessage
) {
}
