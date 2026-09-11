package com.boxai.runtime.workflow.core;

import java.util.Map;

public record WorkflowNodeTrace(
        String nodeId,
        String nodeType,
        String status,
        long durationMs,
        Map<String, Object> output,
        String errorMessage
) {
}
