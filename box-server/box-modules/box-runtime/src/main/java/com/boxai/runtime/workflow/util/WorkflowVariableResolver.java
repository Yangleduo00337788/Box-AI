package com.boxai.runtime.workflow.util;

import com.boxai.runtime.workflow.core.WorkflowExecutionContext;

import java.util.Map;

public final class WorkflowVariableResolver {

    private WorkflowVariableResolver() {
    }

    public static Object resolve(WorkflowExecutionContext context, String key) {
        if (context == null || key == null || key.isBlank()) {
            return null;
        }
        return resolve(key, context.variables());
    }

    public static Object resolve(String key, Map<String, Object> variables) {
        if (variables == null || key == null || key.isBlank()) {
            return null;
        }
        if (variables.containsKey(key)) {
            return variables.get(key);
        }
        if (!key.contains(".")) {
            return null;
        }
        String[] parts = key.split("\\.");
        Object current = variables.get(parts[0]);
        for (int index = 1; index < parts.length && current != null; index++) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(parts[index]);
            } else {
                return null;
            }
        }
        return current;
    }
}
