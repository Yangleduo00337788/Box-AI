package com.boxai.runtime.workflow.core;

@FunctionalInterface
public interface WorkflowStreamCallback {

    void onDelta(String nodeId, String nodeType, String chunk);
}
