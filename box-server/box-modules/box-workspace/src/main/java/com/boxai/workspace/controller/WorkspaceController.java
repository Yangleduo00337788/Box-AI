package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workspace.api.CreateWorkspaceRequest;
import com.boxai.workspace.api.UpdateWorkspaceRequest;
import com.boxai.workspace.api.WorkspaceDetailVO;
import com.boxai.workspace.application.WorkspaceApplicationService;
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
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceApplicationService workspaceApplicationService;
    private final WorkspacePermissionService workspacePermissionService;

    public WorkspaceController(WorkspaceApplicationService workspaceApplicationService,
                               WorkspacePermissionService workspacePermissionService) {
        this.workspaceApplicationService = workspaceApplicationService;
        this.workspacePermissionService = workspacePermissionService;
    }

    @GetMapping
    public Result<List<WorkspaceDetailVO>> list() {
        return Result.success(workspaceApplicationService.listMine(SecurityContexts.currentUser()));
    }

    @PostMapping
    public Result<WorkspaceDetailVO> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return Result.success(workspaceApplicationService.create(SecurityContexts.currentUser(), request));
    }

    @PutMapping("/{id}")
    public Result<WorkspaceDetailVO> update(@PathVariable Long id, @Valid @RequestBody UpdateWorkspaceRequest request) {
        return Result.success(workspaceApplicationService.update(SecurityContexts.currentUser(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workspaceApplicationService.delete(SecurityContexts.currentUser(), id);
        return Result.success();
    }

    @GetMapping("/current-permissions")
    public Result<List<String>> currentPermissions() {
        return Result.success(workspacePermissionService.listCurrentPermissionCodes());
    }
}
