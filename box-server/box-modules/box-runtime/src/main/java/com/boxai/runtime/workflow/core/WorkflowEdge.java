package com.boxai.runtime.workflow.core;

import com.fasterxml.jackson.databind.JsonNode;

public record WorkflowEdge(
        String source,
        String target,
        String sourceHandle,
        JsonNode config
) {
}
