package com.boxai.model.controller;

import com.boxai.common.result.Result;
import com.boxai.model.api.platform.CreatePlatformCredentialRequest;
import com.boxai.model.api.platform.CreatePlatformModelRequest;
import com.boxai.model.api.platform.CreatePlatformProviderRequest;
import com.boxai.model.api.platform.PlatformCredentialVO;
import com.boxai.model.api.platform.PlatformModelVO;
import com.boxai.model.api.platform.PlatformProviderVO;
import com.boxai.model.application.PlatformModelApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/platform")
public class AdminPlatformModelController {

    private final PlatformModelApplicationService platformModelApplicationService;

    public AdminPlatformModelController(PlatformModelApplicationService platformModelApplicationService) {
        this.platformModelApplicationService = platformModelApplicationService;
    }

    @GetMapping("/providers")
    public Result<List<PlatformProviderVO>> listProviders() {
        return Result.success(platformModelApplicationService.listProviders());
    }

    @PostMapping("/providers")
    public Result<PlatformProviderVO> createProvider(@Valid @RequestBody CreatePlatformProviderRequest request) {
        return Result.success(platformModelApplicationService.createProvider(request));
    }

    @GetMapping("/models")
    public Result<List<PlatformModelVO>> listModels() {
        return Result.success(platformModelApplicationService.listModelsAdmin());
    }

    @PostMapping("/models")
    public Result<PlatformModelVO> createModel(@Valid @RequestBody CreatePlatformModelRequest request) {
        return Result.success(platformModelApplicationService.createModel(request));
    }

    @GetMapping("/providers/{providerId}/credentials")
    public Result<List<PlatformCredentialVO>> listCredentials(@PathVariable Long providerId) {
        return Result.success(platformModelApplicationService.listCredentials(providerId));
    }

    @PostMapping("/credentials")
    public Result<PlatformCredentialVO> createCredential(@Valid @RequestBody CreatePlatformCredentialRequest request) {
        return Result.success(platformModelApplicationService.createCredential(request));
    }
}
