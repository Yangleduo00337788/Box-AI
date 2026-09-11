package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.workspace.api.CreateWorkspaceRequest;
import com.boxai.workspace.api.WorkspaceDetailVO;
import com.boxai.workspace.application.WorkspaceApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceApplicationService workspaceApplicationService;

    public WorkspaceController(WorkspaceApplicationService workspaceApplicationService) {
        this.workspaceApplicationService = workspaceApplicationService;
    }

    @GetMapping
    public Result<List<WorkspaceDetailVO>> list() {
        return Result.success(workspaceApplicationService.listMine(SecurityContexts.currentUser()));
    }

    @PostMapping
    public Result<WorkspaceDetailVO> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return Result.success(workspaceApplicationService.create(SecurityContexts.currentUser(), request));
    }
}
