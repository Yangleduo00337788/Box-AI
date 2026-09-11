package com.boxai.runtime.controller;

import com.boxai.common.result.Result;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.boxai.runtime.application.WorkflowExecutionApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/runtime/workflows")
public class WorkflowRuntimeController {

    private final WorkflowExecutionApplicationService workflowExecutionApplicationService;

    public WorkflowRuntimeController(WorkflowExecutionApplicationService workflowExecutionApplicationService) {
        this.workflowExecutionApplicationService = workflowExecutionApplicationService;
    }

    @PostMapping("/{workflowId}/execute")
    public Result<WorkflowExecutionResultVO> execute(@PathVariable Long workflowId,
                                                     @Valid @RequestBody(required = false) WorkflowExecuteRequest request) {
        return Result.success(workflowExecutionApplicationService.execute(
                workflowId, request == null ? new WorkflowExecuteRequest(Map.of()) : request));
    }
}
