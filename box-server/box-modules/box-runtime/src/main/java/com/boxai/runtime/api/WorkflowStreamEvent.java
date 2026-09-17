package com.boxai.runtime.api;

public record WorkflowStreamEvent(String type, String nodeId, String nodeType, Object payload, String message) {

    public static WorkflowStreamEvent nodeStart(String nodeId, String nodeType) {
        return new WorkflowStreamEvent("node.start", nodeId, nodeType, null, null);
    }

    public static WorkflowStreamEvent nodeDelta(String nodeId, String nodeType, String chunk) {
        return new WorkflowStreamEvent("node.delta", nodeId, nodeType, chunk, null);
    }

    public static WorkflowStreamEvent nodeEnd(String nodeId,
                                              String nodeType,
                                              String status,
                                              long durationMs,
                                              Object output,
                                              String errorMessage) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("status", status);
        payload.put("durationMs", durationMs);
        payload.put("output", output == null ? java.util.Map.of() : output);
        payload.put("errorMessage", errorMessage);
        return new WorkflowStreamEvent("node.end", nodeId, nodeType, payload, null);
    }

    public static WorkflowStreamEvent done(Object result) {
        return new WorkflowStreamEvent("done", null, null, result, null);
    }

    public static WorkflowStreamEvent error(String message) {
        return new WorkflowStreamEvent("error", null, null, null, message);
    }
}
