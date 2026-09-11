package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.ReplaceSidebarPinsRequest;
import com.boxai.user.api.SidebarPinsVO;
import com.boxai.user.api.SidebarSelectionVO;
import com.boxai.user.api.UpdateSidebarSelectionRequest;
import com.boxai.user.application.UserSidebarPinApplicationService;
import com.boxai.user.application.UserWorkspaceSelectionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sidebar")
public class SidebarController {

    private final UserSidebarPinApplicationService userSidebarPinApplicationService;
    private final UserWorkspaceSelectionApplicationService userWorkspaceSelectionApplicationService;

    public SidebarController(UserSidebarPinApplicationService userSidebarPinApplicationService,
                             UserWorkspaceSelectionApplicationService userWorkspaceSelectionApplicationService) {
        this.userSidebarPinApplicationService = userSidebarPinApplicationService;
        this.userWorkspaceSelectionApplicationService = userWorkspaceSelectionApplicationService;
    }

    @GetMapping("/pins")
    public Result<SidebarPinsVO> listPins() {
        return Result.success(userSidebarPinApplicationService.list());
    }

    @PutMapping("/pins")
    public Result<SidebarPinsVO> replacePins(@RequestBody ReplaceSidebarPinsRequest request) {
        return Result.success(userSidebarPinApplicationService.replace(request));
    }

    @GetMapping("/selection")
    public Result<SidebarSelectionVO> getSelection() {
        return Result.success(userWorkspaceSelectionApplicationService.get());
    }

    @PutMapping("/selection")
    public Result<SidebarSelectionVO> updateSelection(@RequestBody UpdateSidebarSelectionRequest request) {
        return Result.success(userWorkspaceSelectionApplicationService.update(request));
    }
}
