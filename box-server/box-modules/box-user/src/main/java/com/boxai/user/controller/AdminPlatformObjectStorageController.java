package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.platform.PlatformObjectStorageSettingsVO;
import com.boxai.user.api.platform.TestPlatformObjectStorageRequest;
import com.boxai.user.api.platform.UpdatePlatformObjectStorageRequest;
import com.boxai.user.application.PlatformObjectStorageSettingsApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/platform/object-storage")
public class AdminPlatformObjectStorageController {

    private final PlatformObjectStorageSettingsApplicationService settingsApplicationService;

    public AdminPlatformObjectStorageController(PlatformObjectStorageSettingsApplicationService settingsApplicationService) {
        this.settingsApplicationService = settingsApplicationService;
    }

    @GetMapping
    public Result<PlatformObjectStorageSettingsVO> getSettings() {
        return Result.success(settingsApplicationService.getSettings());
    }

    @PutMapping
    public Result<PlatformObjectStorageSettingsVO> update(@Valid @RequestBody UpdatePlatformObjectStorageRequest request) {
        return Result.success(settingsApplicationService.update(request));
    }

    @PostMapping("/test")
    public Result<Map<String, Boolean>> test(@Valid @RequestBody TestPlatformObjectStorageRequest request) {
        return Result.success(Map.of("ok", settingsApplicationService.testConnection(request)));
    }
}
