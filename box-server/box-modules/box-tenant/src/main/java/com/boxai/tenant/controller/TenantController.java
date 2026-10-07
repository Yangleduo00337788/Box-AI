package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.security.context.SecurityContexts;
import com.boxai.tenant.api.EnterpriseOrgAccessVO;
import com.boxai.tenant.api.TenantVO;
import com.boxai.tenant.api.UpdateOrgIdRequest;
import com.boxai.tenant.api.UpgradeEnterpriseRequest;
import com.boxai.tenant.application.TenantApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/org-access")
    public Result<EnterpriseOrgAccessVO> orgAccess() {
        return Result.success(tenantApplicationService.getOrgAccess(SecurityContexts.currentUser().userId()));
    }

    @PostMapping("/invite-code/rotate")
    public Result<EnterpriseOrgAccessVO> rotateInviteCode() {
        return Result.success(tenantApplicationService.rotateInviteCode(SecurityContexts.currentUser().userId()));
    }

    @PutMapping("/org-id")
    public Result<EnterpriseOrgAccessVO> updateOrgId(@Valid @RequestBody UpdateOrgIdRequest request) {
        return Result.success(tenantApplicationService.updateOrgId(
                SecurityContexts.currentUser().userId(),
                request.orgId()));
    }

    @PostMapping("/upgrade-enterprise")
    public Result<TenantVO> upgradeEnterprise(@Valid @RequestBody UpgradeEnterpriseRequest request) {
        return Result.success(tenantApplicationService.upgradeToEnterprise(
                SecurityContexts.currentUser().userId(),
                request.companyName()));
    }
}
