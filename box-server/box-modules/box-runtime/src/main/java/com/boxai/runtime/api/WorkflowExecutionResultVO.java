package com.boxai.runtime.api;

import java.util.List;
import java.util.Map;

public record WorkflowExecutionResultVO(
        Long executionId,
        String executionNo,
        String status,
        Long workflowId,
        Long workflowVersionId,
        Map<String, Object> outputs,
        List<WorkflowNodeTraceVO> nodeTraces,
        String errorMessage
) {
}
