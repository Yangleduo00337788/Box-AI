package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.tenant.api.TenantVO;
import com.boxai.tenant.api.UpgradeEnterpriseRequest;
import com.boxai.tenant.application.TenantApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant")
public class TenantController {

    private final TenantApplicationService tenantApplicationService;

    public TenantController(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
    }

    @PostMapping("/upgrade-enterprise")
    public Result<TenantVO> upgradeEnterprise(@Valid @RequestBody UpgradeEnterpriseRequest request) {
        return Result.success(tenantApplicationService.upgradeToEnterprise(
                SecurityContexts.currentUser().userId(),
                request.companyName()));
    }
}
