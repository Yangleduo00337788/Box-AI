package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.BillingInvoiceVO;
import com.boxai.tenant.application.SubscriptionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/billing")
public class AdminBillingController {

    private final SubscriptionApplicationService subscriptionApplicationService;

    public AdminBillingController(SubscriptionApplicationService subscriptionApplicationService) {
        this.subscriptionApplicationService = subscriptionApplicationService;
    }

    @GetMapping("/invoices")
    public Result<List<BillingInvoiceVO>> invoices() {
        return Result.success(subscriptionApplicationService.listAllInvoicesForAdmin());
    }
}
