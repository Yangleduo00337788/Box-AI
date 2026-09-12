package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.workspace.api.InviteWorkspaceMemberRequest;
import com.boxai.workspace.api.UpdateWorkspaceMemberRoleRequest;
import com.boxai.workspace.api.WorkspaceMemberVO;
import com.boxai.workspace.application.WorkspaceMemberApplicationService;
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
@RequestMapping("/api/v1/workspace/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberApplicationService workspaceMemberApplicationService;

    public WorkspaceMemberController(WorkspaceMemberApplicationService workspaceMemberApplicationService) {
        this.workspaceMemberApplicationService = workspaceMemberApplicationService;
    }

    @GetMapping
    public Result<List<WorkspaceMemberVO>> list() {
        return Result.success(workspaceMemberApplicationService.listCurrentWorkspaceMembers());
    }

    @PostMapping
    public Result<WorkspaceMemberVO> invite(@Valid @RequestBody InviteWorkspaceMemberRequest request) {
        return Result.success(workspaceMemberApplicationService.invite(request));
    }

    @PutMapping("/{userId}/role")
    public Result<WorkspaceMemberVO> updateRole(@PathVariable Long userId,
                                                @Valid @RequestBody UpdateWorkspaceMemberRoleRequest request) {
        return Result.success(workspaceMemberApplicationService.updateRole(userId, request));
    }

    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId) {
        workspaceMemberApplicationService.remove(userId);
        return Result.success();
    }
}
