package com.boxai.runtime.workflow.core;

public record NodeExecutionContext(
        WorkflowGraph graph,
        WorkflowExecutionContext executionContext,
        WorkflowNode node
) {
}
