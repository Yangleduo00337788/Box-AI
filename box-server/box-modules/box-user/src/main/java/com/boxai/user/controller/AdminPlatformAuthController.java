package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.ResetPasswordRequest;
import com.boxai.user.api.SendVerificationCodeRequest;
import com.boxai.user.api.SendVerificationCodeResponse;
import com.boxai.user.application.AdminPlatformAuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminPlatformAuthController {

    private final AdminPlatformAuthApplicationService adminPlatformAuthApplicationService;

    public AdminPlatformAuthController(AdminPlatformAuthApplicationService adminPlatformAuthApplicationService) {
        this.adminPlatformAuthApplicationService = adminPlatformAuthApplicationService;
    }

    @PostMapping("/verification-code")
    public Result<SendVerificationCodeResponse> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        return Result.success(adminPlatformAuthApplicationService.sendVerificationCode(request));
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        adminPlatformAuthApplicationService.resetPassword(request);
        return Result.success(null);
    }
}
