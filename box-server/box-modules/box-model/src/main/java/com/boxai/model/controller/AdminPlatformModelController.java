package com.boxai.model.controller;

import com.boxai.common.result.Result;
import com.boxai.model.api.platform.CreatePlatformCredentialRequest;
import com.boxai.model.api.platform.CreatePlatformModelRequest;
import com.boxai.model.api.platform.CreatePlatformProviderRequest;
import com.boxai.model.api.platform.ImportPlatformModelsRequest;
import com.boxai.model.api.platform.PlatformCredentialVO;
import com.boxai.model.api.platform.PlatformModelVO;
import com.boxai.model.api.platform.PlatformProviderVO;
import com.boxai.model.api.platform.UpdatePlatformModelRequest;
import com.boxai.model.api.platform.UpdatePlatformProviderRequest;
import com.boxai.model.api.platform.PlatformOcrDefaultVO;
import com.boxai.model.api.platform.UpdatePlatformOcrDefaultRequest;
import com.boxai.model.api.platform.UpstreamModelVO;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.application.PlatformOcrSettingsApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/platform")
public class AdminPlatformModelController {

    private final PlatformModelApplicationService platformModelApplicationService;
    private final PlatformOcrSettingsApplicationService platformOcrSettingsApplicationService;

    public AdminPlatformModelController(PlatformModelApplicationService platformModelApplicationService,
                                        PlatformOcrSettingsApplicationService platformOcrSettingsApplicationService) {
        this.platformModelApplicationService = platformModelApplicationService;
        this.platformOcrSettingsApplicationService = platformOcrSettingsApplicationService;
    }

    @GetMapping("/providers")
    public Result<List<PlatformProviderVO>> listProviders() {
        return Result.success(platformModelApplicationService.listProviders());
    }

    @PostMapping("/providers")
    public Result<PlatformProviderVO> createProvider(@Valid @RequestBody CreatePlatformProviderRequest request) {
        return Result.success(platformModelApplicationService.createProvider(request));
    }

    @PutMapping("/providers/{id}")
    public Result<PlatformProviderVO> updateProvider(@PathVariable Long id,
                                                     @Valid @RequestBody UpdatePlatformProviderRequest request) {
        return Result.success(platformModelApplicationService.updateProvider(id, request));
    }

    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable Long id) {
        platformModelApplicationService.deleteProvider(id);
        return Result.success(null);
    }

    @GetMapping("/models")
    public Result<List<PlatformModelVO>> listModels(@RequestParam(required = false) Long providerId) {
        return Result.success(platformModelApplicationService.listModelsAdmin(providerId));
    }

    @PostMapping("/models")
    public Result<PlatformModelVO> createModel(@Valid @RequestBody CreatePlatformModelRequest request) {
        return Result.success(platformModelApplicationService.createModel(request));
    }

    @PutMapping("/models/{id}")
    public Result<PlatformModelVO> updateModel(@PathVariable Long id,
                                               @Valid @RequestBody UpdatePlatformModelRequest request) {
        return Result.success(platformModelApplicationService.updateModel(id, request));
    }

    @DeleteMapping("/models/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        platformModelApplicationService.deleteModel(id);
        return Result.success(null);
    }

    @GetMapping("/providers/{providerId}/upstream-models")
    public Result<List<UpstreamModelVO>> listUpstreamModels(@PathVariable Long providerId) {
        return Result.success(platformModelApplicationService.listUpstreamModels(providerId));
    }

    @PostMapping("/providers/{providerId}/models/import")
    public Result<List<PlatformModelVO>> importUpstreamModels(@PathVariable Long providerId,
                                                              @Valid @RequestBody ImportPlatformModelsRequest request) {
        return Result.success(platformModelApplicationService.importUpstreamModels(providerId, request));
    }

    @PostMapping("/providers/{providerId}/models/sync-limits")
    public Result<Integer> syncMissingModelLimits(@PathVariable Long providerId) {
        return Result.success(platformModelApplicationService.syncMissingModelLimits(providerId));
    }

    @GetMapping("/providers/{providerId}/credentials")
    public Result<List<PlatformCredentialVO>> listCredentials(@PathVariable Long providerId) {
        return Result.success(platformModelApplicationService.listCredentials(providerId));
    }

    @PostMapping("/credentials")
    public Result<PlatformCredentialVO> createCredential(@Valid @RequestBody CreatePlatformCredentialRequest request) {
        return Result.success(platformModelApplicationService.createCredential(request));
    }

    @DeleteMapping("/credentials/{id}")
    public Result<Void> deleteCredential(@PathVariable Long id) {
        platformModelApplicationService.deleteCredential(id);
        return Result.success(null);
    }

    @GetMapping("/ocr-default")
    public Result<PlatformOcrDefaultVO> getOcrDefault() {
        return Result.success(platformOcrSettingsApplicationService.getSettings());
    }

    @PutMapping("/ocr-default")
    public Result<PlatformOcrDefaultVO> updateOcrDefault(@Valid @RequestBody UpdatePlatformOcrDefaultRequest request) {
        return Result.success(platformOcrSettingsApplicationService.update(request));
    }
}
