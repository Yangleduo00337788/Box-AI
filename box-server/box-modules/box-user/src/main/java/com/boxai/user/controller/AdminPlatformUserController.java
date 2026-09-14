package com.boxai.user.controller;

import com.boxai.common.result.PageResult;
import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.user.api.CreatePlatformAdminRequest;
import com.boxai.user.api.PlatformUserVO;
import com.boxai.user.api.UpdateUserStatusRequest;
import com.boxai.user.application.AdminPlatformUserApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminPlatformUserController {

    private final AdminPlatformUserApplicationService adminPlatformUserApplicationService;

    public AdminPlatformUserController(AdminPlatformUserApplicationService adminPlatformUserApplicationService) {
        this.adminPlatformUserApplicationService = adminPlatformUserApplicationService;
    }

    @GetMapping
    public Result<PageResult<PlatformUserVO>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String userType,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminPlatformUserApplicationService.page(keyword, userType, status, page, pageSize));
    }

    @PostMapping
    public Result<PlatformUserVO> create(@Valid @RequestBody CreatePlatformAdminRequest request) {
        return Result.success(adminPlatformUserApplicationService.createAdmin(request));
    }

    @PutMapping("/{id}/status")
    public Result<PlatformUserVO> updateStatus(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserStatusRequest request) {
        return Result.success(adminPlatformUserApplicationService.updateStatus(
                SecurityContexts.currentUser(), id, request.status()));
    }
}
