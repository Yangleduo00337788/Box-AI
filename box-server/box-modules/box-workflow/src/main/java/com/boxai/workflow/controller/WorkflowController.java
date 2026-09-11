package com.boxai.workflow.controller;

import com.boxai.common.result.Result;
import com.boxai.workflow.api.CreateWorkflowRequest;
import com.boxai.workflow.api.UpdateWorkflowDefinitionRequest;
import com.boxai.workflow.api.UpdateWorkflowRequest;
import com.boxai.workflow.api.WorkflowPublishVO;
import com.boxai.workflow.api.WorkflowVO;
import com.boxai.workflow.api.WorkflowValidateVO;
import com.boxai.workflow.application.WorkflowApplicationService;
import com.boxai.workflow.application.WorkflowPublishApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowApplicationService workflowApplicationService;
    private final WorkflowPublishApplicationService workflowPublishApplicationService;

    public WorkflowController(WorkflowApplicationService workflowApplicationService,
                              WorkflowPublishApplicationService workflowPublishApplicationService) {
        this.workflowApplicationService = workflowApplicationService;
        this.workflowPublishApplicationService = workflowPublishApplicationService;
    }

    @GetMapping
    public Result<List<WorkflowVO>> list() {
        return Result.success(workflowApplicationService.list());
    }

    @PostMapping
    public Result<WorkflowVO> create(@Valid @RequestBody CreateWorkflowRequest request) {
        return Result.success(workflowApplicationService.create(request));
    }

    @GetMapping("/{id}")
    public Result<WorkflowVO> detail(@PathVariable Long id) {
        return Result.success(workflowApplicationService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<WorkflowVO> update(@PathVariable Long id, @Valid @RequestBody UpdateWorkflowRequest request) {
        return Result.success(workflowApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowApplicationService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/definition")
    public Result<WorkflowVO> updateDefinition(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateWorkflowDefinitionRequest request) {
        return Result.success(workflowApplicationService.updateDefinition(id, request));
    }

    @PostMapping("/{id}/validate")
    public Result<WorkflowValidateVO> validate(@PathVariable Long id) {
        return Result.success(workflowApplicationService.validate(id));
    }

    @GetMapping("/{id}/publish")
    public Result<WorkflowPublishVO> publishStatus(@PathVariable Long id) {
        return Result.success(workflowPublishApplicationService.getPublishStatus(id));
    }

    @PostMapping("/{id}/publish")
    public Result<WorkflowPublishVO> publish(@PathVariable Long id) {
        return Result.success(workflowPublishApplicationService.publish(id));
    }
}
