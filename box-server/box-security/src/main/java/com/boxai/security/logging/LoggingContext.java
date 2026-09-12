package com.boxai.security.logging;

import org.slf4j.MDC;

public final class LoggingContext {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String WORKSPACE_ID = "workspaceId";
    public static final String EXECUTION_ID = "executionId";
    public static final String TRACE_ID = "traceId";

    private LoggingContext() {
    }

    public static void setRequestId(String requestId) {
        put(REQUEST_ID, requestId);
    }

    public static void setUserId(Long userId) {
        put(USER_ID, userId);
    }

    public static void setWorkspaceId(Long workspaceId) {
        put(WORKSPACE_ID, workspaceId);
    }

    public static void setExecutionId(Long executionId) {
        put(EXECUTION_ID, executionId);
    }

    public static void setTraceId(String traceId) {
        put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.clear();
    }

    private static void put(String key, Object value) {
        if (value == null) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, String.valueOf(value));
    }
}
