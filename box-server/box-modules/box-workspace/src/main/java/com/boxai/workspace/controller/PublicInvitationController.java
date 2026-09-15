package com.boxai.workspace.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.workspace.api.WorkspaceInvitationVO;
import com.boxai.workspace.api.WorkspaceMemberVO;
import com.boxai.workspace.application.WorkspaceInvitationApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/invitations")
public class PublicInvitationController {

    private final WorkspaceInvitationApplicationService invitationApplicationService;

    public PublicInvitationController(WorkspaceInvitationApplicationService invitationApplicationService) {
        this.invitationApplicationService = invitationApplicationService;
    }

    @GetMapping("/{token}")
    public Result<WorkspaceInvitationVO> preview(@PathVariable String token) {
        return Result.success(invitationApplicationService.preview(token));
    }

    @PostMapping("/{token}/accept")
    public Result<WorkspaceMemberVO> accept(@PathVariable String token) {
        return Result.success(invitationApplicationService.accept(token, SecurityContexts.currentUser().userId()));
    }
}
