package com.boxai.user.controller;

import com.boxai.common.result.PageResult;
import com.boxai.common.result.Result;
import com.boxai.user.api.AuditLogVO;
import com.boxai.user.application.AdminAuditLogApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogApplicationService adminAuditLogApplicationService;

    public AdminAuditLogController(AdminAuditLogApplicationService adminAuditLogApplicationService) {
        this.adminAuditLogApplicationService = adminAuditLogApplicationService;
    }

    @GetMapping
    public Result<PageResult<AuditLogVO>> page(@RequestParam(required = false) String action,
                                               @RequestParam(required = false) String resourceType,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminAuditLogApplicationService.page(
                action, resourceType, userId, startTime, endTime, page, pageSize));
    }
}
