package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.SystemContentVO;
import com.boxai.user.application.SystemContentApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemContentController {

    private final SystemContentApplicationService systemContentApplicationService;

    public SystemContentController(SystemContentApplicationService systemContentApplicationService) {
        this.systemContentApplicationService = systemContentApplicationService;
    }

    @GetMapping("/content")
    public Result<SystemContentVO> content() {
        return Result.success(systemContentApplicationService.getContent());
    }
}
