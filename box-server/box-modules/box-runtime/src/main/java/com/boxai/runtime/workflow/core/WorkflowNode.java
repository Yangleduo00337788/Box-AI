package com.boxai.runtime.workflow.core;

import com.fasterxml.jackson.databind.JsonNode;

public record WorkflowNode(
        String id,
        String type,
        JsonNode config
) {
}
