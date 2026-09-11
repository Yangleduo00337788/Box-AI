package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.AdminSystemConfigVO;
import com.boxai.user.api.UpdateSystemConfigRequest;
import com.boxai.user.application.AdminSystemConfigApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system/config")
public class AdminSystemConfigController {

    private final AdminSystemConfigApplicationService adminSystemConfigApplicationService;

    public AdminSystemConfigController(AdminSystemConfigApplicationService adminSystemConfigApplicationService) {
        this.adminSystemConfigApplicationService = adminSystemConfigApplicationService;
    }

    @GetMapping
    public Result<List<AdminSystemConfigVO>> list() {
        return Result.success(adminSystemConfigApplicationService.list());
    }

    @PutMapping
    public Result<AdminSystemConfigVO> upsert(@Valid @RequestBody UpdateSystemConfigRequest request) {
        return Result.success(adminSystemConfigApplicationService.upsert(request));
    }
}
