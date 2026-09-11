package com.boxai.runtime.workflow.core;

import java.util.List;
import java.util.Map;

public record WorkflowExecutionResult(
        Long executionId,
        String executionNo,
        String status,
        Map<String, Object> outputs,
        List<WorkflowNodeTrace> nodeTraces,
        String errorMessage
) {
}
