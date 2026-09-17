package com.boxai.runtime.workflow.core;

import java.util.ArrayList;
import java.util.List;

public record WorkflowBranchResult(
        boolean succeeded,
        List<WorkflowNodeTrace> traces,
        String errorMessage
) {

    public static WorkflowBranchResult success(List<WorkflowNodeTrace> traces) {
        return new WorkflowBranchResult(true, traces == null ? List.of() : traces, null);
    }

    public static WorkflowBranchResult failure(List<WorkflowNodeTrace> traces, String errorMessage) {
        return new WorkflowBranchResult(false, traces == null ? new ArrayList<>() : traces, errorMessage);
    }
}
