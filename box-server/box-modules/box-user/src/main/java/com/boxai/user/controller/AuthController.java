package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.user.api.AuthVO;
import com.boxai.user.api.ChangePasswordRequest;
import com.boxai.user.api.LoginRequest;
import com.boxai.user.api.RegisterRequest;
import com.boxai.user.api.UpdateProfileRequest;
import com.boxai.user.api.UpdateUserPreferenceRequest;
import com.boxai.user.api.UserPreferenceVO;
import com.boxai.user.application.AuthApplicationService;
import com.boxai.user.application.UserPreferenceApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final UserPreferenceApplicationService userPreferenceApplicationService;

    public AuthController(AuthApplicationService authApplicationService,
                          UserPreferenceApplicationService userPreferenceApplicationService) {
        this.authApplicationService = authApplicationService;
        this.userPreferenceApplicationService = userPreferenceApplicationService;
    }

    @PostMapping("/register")
    public Result<AuthVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authApplicationService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authApplicationService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthVO> me() {
        return Result.success(authApplicationService.me(SecurityContexts.currentUser()));
    }

    @PutMapping("/profile")
    public Result<AuthVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(authApplicationService.updateProfile(SecurityContexts.currentUser(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authApplicationService.updatePassword(SecurityContexts.currentUser().userId(), request);
        return Result.success(null);
    }

    @GetMapping("/preferences")
    public Result<UserPreferenceVO> getPreferences() {
        return Result.success(userPreferenceApplicationService.get(SecurityContexts.currentUser().userId()));
    }

    @PutMapping("/preferences")
    public Result<UserPreferenceVO> updatePreferences(@Valid @RequestBody UpdateUserPreferenceRequest request) {
        return Result.success(userPreferenceApplicationService.update(SecurityContexts.currentUser().userId(), request));
    }
}
