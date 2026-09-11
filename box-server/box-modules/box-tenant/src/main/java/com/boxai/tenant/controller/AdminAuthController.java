package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.tenant.api.AdminAuthVO;
import com.boxai.tenant.api.AdminLoginRequest;
import com.boxai.tenant.application.AdminAuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthApplicationService adminAuthApplicationService;

    public AdminAuthController(AdminAuthApplicationService adminAuthApplicationService) {
        this.adminAuthApplicationService = adminAuthApplicationService;
    }

    @PostMapping("/login")
    public Result<AdminAuthVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.success(adminAuthApplicationService.login(request));
    }

    @GetMapping("/me")
    public Result<AdminAuthVO> me() {
        return Result.success(adminAuthApplicationService.me(SecurityContexts.currentUser()));
    }
}
