package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.PlanVO;
import com.boxai.tenant.application.SubscriptionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final SubscriptionApplicationService subscriptionApplicationService;

    public PlanController(SubscriptionApplicationService subscriptionApplicationService) {
        this.subscriptionApplicationService = subscriptionApplicationService;
    }

    @GetMapping
    public Result<List<PlanVO>> list() {
        return Result.success(subscriptionApplicationService.listPublicPlans());
    }
}
