package com.boxai.runtime.workflow.core;

@FunctionalInterface
public interface WorkflowExecutionListener {

    void onNodeStart(String nodeId, String nodeType);

    default void onNodeComplete(WorkflowNodeTrace trace) {
    }

    default void onNodeDelta(String nodeId, String nodeType, String chunk) {
    }
}
