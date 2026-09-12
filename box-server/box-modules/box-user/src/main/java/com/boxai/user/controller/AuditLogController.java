package com.boxai.user.controller;

import com.boxai.common.result.PageResult;
import com.boxai.common.result.Result;
import com.boxai.user.api.AuditLogVO;
import com.boxai.user.application.AuditLogApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogApplicationService auditLogApplicationService;

    public AuditLogController(AuditLogApplicationService auditLogApplicationService) {
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping
    public Result<PageResult<AuditLogVO>> page(@RequestParam(required = false) String action,
                                               @RequestParam(required = false) String resourceType,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(auditLogApplicationService.page(
                action, resourceType, userId, startTime, endTime, page, pageSize));
    }
}
