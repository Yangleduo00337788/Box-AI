package com.boxai.runtime.workflow.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WorkflowExecutionContext {

    private final Long workflowId;
    private final Long workflowVersionId;
    private final Long executionId;
    private final String executionNo;
    private final boolean debugMode;
    private final int depth;
    private final Set<Long> callStack;
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private final Map<String, Object> outputs = new LinkedHashMap<>();
    private WorkflowExecutionListener listener;
    private WorkflowStreamCallback streamCallback;

    public WorkflowExecutionContext(Long workflowId,
                                    Long workflowVersionId,
                                    Long executionId,
                                    String executionNo,
                                    Map<String, Object> inputs) {
        this(workflowId, workflowVersionId, executionId, executionNo, inputs, false, 0, rootCallStack(workflowId));
    }

    public WorkflowExecutionContext(Long workflowId,
                                    Long workflowVersionId,
                                    Long executionId,
                                    String executionNo,
                                    Map<String, Object> inputs,
                                    boolean debugMode) {
        this(workflowId, workflowVersionId, executionId, executionNo, inputs, debugMode, 0, rootCallStack(workflowId));
    }

    private WorkflowExecutionContext(Long workflowId,
                                     Long workflowVersionId,
                                     Long executionId,
                                     String executionNo,
                                     Map<String, Object> inputs,
                                     boolean debugMode,
                                     int depth,
                                     Set<Long> callStack) {
        this.workflowId = workflowId;
        this.workflowVersionId = workflowVersionId;
        this.executionId = executionId;
        this.executionNo = executionNo;
        this.debugMode = debugMode;
        this.depth = depth;
        this.callStack = callStack;
        if (inputs != null) {
            this.variables.putAll(inputs);
        }
    }

    public static WorkflowExecutionContext child(WorkflowExecutionContext parent,
                                                 Long workflowId,
                                                 Long workflowVersionId,
                                                 Map<String, Object> inputs) {
        Set<Long> stack = new LinkedHashSet<>(parent.callStack);
        stack.add(workflowId);
        return new WorkflowExecutionContext(
                workflowId,
                workflowVersionId,
                parent.executionId,
                parent.executionNo,
                inputs,
                parent.debugMode,
                parent.depth + 1,
                stack);
    }

    private static Set<Long> rootCallStack(Long workflowId) {
        Set<Long> stack = new LinkedHashSet<>();
        if (workflowId != null) {
            stack.add(workflowId);
        }
        return stack;
    }

    public Long workflowId() {
        return workflowId;
    }

    public Long workflowVersionId() {
        return workflowVersionId;
    }

    public Long executionId() {
        return executionId;
    }

    public String executionNo() {
        return executionNo;
    }

    public boolean debugMode() {
        return debugMode;
    }

    public int depth() {
        return depth;
    }

    public Set<Long> callStack() {
        return callStack;
    }

    public Map<String, Object> variables() {
        return variables;
    }

    public Map<String, Object> outputs() {
        return outputs;
    }

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public void setOutput(String name, Object value) {
        outputs.put(name, value);
    }

    public WorkflowExecutionListener listener() {
        return listener;
    }

    public void setListener(WorkflowExecutionListener listener) {
        this.listener = listener;
    }

    public WorkflowStreamCallback streamCallback() {
        return streamCallback;
    }

    public void setStreamCallback(WorkflowStreamCallback streamCallback) {
        this.streamCallback = streamCallback;
    }

    public WorkflowExecutionContext snapshot() {
        WorkflowExecutionContext copy = new WorkflowExecutionContext(
                workflowId,
                workflowVersionId,
                executionId,
                executionNo,
                new LinkedHashMap<>(variables),
                debugMode,
                depth,
                new LinkedHashSet<>(callStack));
        copy.listener = listener;
        copy.streamCallback = streamCallback;
        copy.outputs.putAll(outputs);
        return copy;
    }

    public void mergeFrom(WorkflowExecutionContext branch) {
        if (branch == null) {
            return;
        }
        variables.putAll(branch.variables);
        outputs.putAll(branch.outputs);
    }
}
