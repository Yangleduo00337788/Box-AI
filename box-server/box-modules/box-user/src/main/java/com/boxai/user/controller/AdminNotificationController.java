package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.AdminDismissNotificationRequest;
import com.boxai.user.api.AdminInboxVO;
import com.boxai.user.application.AdminNotificationApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationController {

    private final AdminNotificationApplicationService adminNotificationApplicationService;

    public AdminNotificationController(AdminNotificationApplicationService adminNotificationApplicationService) {
        this.adminNotificationApplicationService = adminNotificationApplicationService;
    }

    @GetMapping("/inbox")
    public Result<AdminInboxVO> inbox() {
        return Result.success(adminNotificationApplicationService.inbox());
    }

    @PostMapping("/read")
    public Result<Void> markRead(@Valid @RequestBody AdminDismissNotificationRequest request) {
        adminNotificationApplicationService.markRead(request.key());
        return Result.success(null);
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        adminNotificationApplicationService.markAllRead();
        return Result.success(null);
    }

    @PostMapping("/dismiss")
    public Result<Void> dismiss(@Valid @RequestBody AdminDismissNotificationRequest request) {
        adminNotificationApplicationService.dismiss(request.key());
        return Result.success(null);
    }
}
