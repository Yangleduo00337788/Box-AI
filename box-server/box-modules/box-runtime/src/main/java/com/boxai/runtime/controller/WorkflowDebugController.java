package com.boxai.runtime.controller;

import com.boxai.common.result.Result;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.boxai.runtime.application.WorkflowExecutionApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowDebugController {

    private final WorkflowExecutionApplicationService workflowExecutionApplicationService;

    public WorkflowDebugController(WorkflowExecutionApplicationService workflowExecutionApplicationService) {
        this.workflowExecutionApplicationService = workflowExecutionApplicationService;
    }

    @PostMapping("/{id}/debug")
    public Result<WorkflowExecutionResultVO> debug(@PathVariable Long id,
                                                   @Valid @RequestBody(required = false) WorkflowExecuteRequest request) {
        return Result.success(workflowExecutionApplicationService.debug(
                id, request == null ? new WorkflowExecuteRequest(Map.of()) : request));
    }

    @PostMapping("/{id}/debug/stream")
    public SseEmitter debugStream(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) WorkflowExecuteRequest request,
                                  HttpServletResponse response) {
        return workflowExecutionApplicationService.debugStream(
                id, request == null ? new WorkflowExecuteRequest(Map.of()) : request, response);
    }
}
