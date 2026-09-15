package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.workspace.api.InviteWorkspaceMemberRequest;
import com.boxai.workspace.api.WorkspaceInvitationVO;
import com.boxai.workspace.api.WorkspaceMemberVO;
import com.boxai.workspace.application.WorkspaceInvitationApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspace/invitations")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationApplicationService invitationApplicationService;

    public WorkspaceInvitationController(WorkspaceInvitationApplicationService invitationApplicationService) {
        this.invitationApplicationService = invitationApplicationService;
    }

    @GetMapping
    public Result<List<WorkspaceInvitationVO>> listPending() {
        return Result.success(invitationApplicationService.listPending());
    }

    @PostMapping
    public Result<WorkspaceInvitationVO> create(@Valid @RequestBody InviteWorkspaceMemberRequest request) {
        return Result.success(invitationApplicationService.createInvite(request));
    }

    @PostMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        invitationApplicationService.revoke(id);
        return Result.success();
    }
}
