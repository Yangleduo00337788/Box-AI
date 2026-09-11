package com.boxai.runtime.workflow.core;

import java.util.Map;

public record NodeExecutionResult(
        boolean succeeded,
        boolean terminal,
        String branchHandle,
        Map<String, Object> nodeOutput,
        String errorMessage
) {

    public static NodeExecutionResult ok() {
        return new NodeExecutionResult(true, false, null, Map.of(), null);
    }

    public static NodeExecutionResult ok(Map<String, Object> nodeOutput) {
        return new NodeExecutionResult(true, false, null, nodeOutput == null ? Map.of() : nodeOutput, null);
    }

    public static NodeExecutionResult okWithBranch(String branchHandle) {
        return new NodeExecutionResult(true, false, branchHandle, Map.of(), null);
    }

    public static NodeExecutionResult terminal(Map<String, Object> nodeOutput) {
        return new NodeExecutionResult(true, true, null, nodeOutput == null ? Map.of() : nodeOutput, null);
    }

    public static NodeExecutionResult failed(String errorMessage) {
        return new NodeExecutionResult(false, false, null, Map.of(), errorMessage);
    }
}
