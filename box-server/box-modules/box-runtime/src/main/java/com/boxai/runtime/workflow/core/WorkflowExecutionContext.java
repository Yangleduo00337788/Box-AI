package com.boxai.runtime.workflow.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowExecutionContext {

    private final Long workflowId;
    private final Long workflowVersionId;
    private final Long executionId;
    private final String executionNo;
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private final Map<String, Object> outputs = new LinkedHashMap<>();

    public WorkflowExecutionContext(Long workflowId,
                                    Long workflowVersionId,
                                    Long executionId,
                                    String executionNo,
                                    Map<String, Object> inputs) {
        this.workflowId = workflowId;
        this.workflowVersionId = workflowVersionId;
        this.executionId = executionId;
        this.executionNo = executionNo;
        if (inputs != null) {
            this.variables.putAll(inputs);
        }
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
}
