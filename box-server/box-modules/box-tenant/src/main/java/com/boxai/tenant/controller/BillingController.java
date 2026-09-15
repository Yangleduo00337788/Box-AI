package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.BillingInvoiceVO;
import com.boxai.tenant.api.BillingOverviewVO;
import com.boxai.tenant.api.CreateSubscriptionOrderVO;
import com.boxai.tenant.api.PaymentRecordVO;
import com.boxai.tenant.api.SubscribePlanRequest;
import com.boxai.tenant.application.BillingApplicationService;
import com.boxai.tenant.application.SubscriptionApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingApplicationService billingApplicationService;
    private final SubscriptionApplicationService subscriptionApplicationService;

    public BillingController(BillingApplicationService billingApplicationService,
                             SubscriptionApplicationService subscriptionApplicationService) {
        this.billingApplicationService = billingApplicationService;
        this.subscriptionApplicationService = subscriptionApplicationService;
    }

    @GetMapping("/overview")
    public Result<BillingOverviewVO> overview() {
        return Result.success(billingApplicationService.overviewForCurrentWorkspace());
    }

    @PostMapping("/subscribe")
    public Result<CreateSubscriptionOrderVO> subscribe(@Valid @RequestBody SubscribePlanRequest request) {
        return Result.success(subscriptionApplicationService.subscribe(request));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public Result<PaymentRecordVO> confirmPayment(@PathVariable Long paymentId) {
        return Result.success(subscriptionApplicationService.confirmPayment(paymentId));
    }

    @GetMapping("/invoices")
    public Result<List<BillingInvoiceVO>> invoices() {
        return Result.success(subscriptionApplicationService.listMyInvoices());
    }
}
