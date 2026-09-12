package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.BillingOverviewVO;
import com.boxai.tenant.application.BillingApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingApplicationService billingApplicationService;

    public BillingController(BillingApplicationService billingApplicationService) {
        this.billingApplicationService = billingApplicationService;
    }

    @GetMapping("/overview")
    public Result<BillingOverviewVO> overview() {
        return Result.success(billingApplicationService.overviewForCurrentWorkspace());
    }
}
