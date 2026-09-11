package com.boxai.model.controller;

import com.boxai.common.result.Result;
import com.boxai.model.api.platform.PlatformModelVO;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformModelController {

    private final PlatformModelApplicationService platformModelApplicationService;

    public PlatformModelController(PlatformModelApplicationService platformModelApplicationService) {
        this.platformModelApplicationService = platformModelApplicationService;
    }

    @GetMapping("/models")
    public Result<List<PlatformModelVO>> listModels() {
        return Result.success(platformModelApplicationService.listModelsForConsumer());
    }

    @GetMapping("/capabilities")
    public Result<Map<String, Boolean>> capabilities() {
        LoginUser user = SecurityContexts.currentUser();
        return Result.success(Map.of(
                "byokEnabled", platformModelApplicationService.isByokEnabledForUser(user.userId())));
    }
}
