package com.boxai.runtime.api;

import java.util.Map;

public record WorkflowExecuteRequest(
        Map<String, Object> inputs
) {
}
