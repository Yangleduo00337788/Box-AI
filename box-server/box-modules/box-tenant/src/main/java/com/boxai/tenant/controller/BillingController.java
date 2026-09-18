package com.boxai.tenant.controller;

import com.boxai.common.result.Result;
import com.boxai.tenant.api.BillingInvoiceVO;
import com.boxai.tenant.api.BillingOverviewVO;
import com.boxai.tenant.api.CreateSubscriptionOrderVO;
import com.boxai.tenant.api.PaymentRecordVO;
import com.boxai.tenant.api.SubscribePlanRequest;
import com.boxai.tenant.application.BillingApplicationService;
import com.boxai.tenant.application.SubscriptionApplicationService;
import com.boxai.tenant.payment.AlipayPaymentGateway;
import com.boxai.tenant.payment.PaymentProperties;
import com.boxai.tenant.payment.StripePaymentGateway;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingApplicationService billingApplicationService;
    private final SubscriptionApplicationService subscriptionApplicationService;
    private final PaymentProperties paymentProperties;
    private final StripePaymentGateway stripePaymentGateway;
    private final AlipayPaymentGateway alipayPaymentGateway;

    public BillingController(BillingApplicationService billingApplicationService,
                             SubscriptionApplicationService subscriptionApplicationService,
                             PaymentProperties paymentProperties,
                             StripePaymentGateway stripePaymentGateway,
                             AlipayPaymentGateway alipayPaymentGateway) {
        this.billingApplicationService = billingApplicationService;
        this.subscriptionApplicationService = subscriptionApplicationService;
        this.paymentProperties = paymentProperties;
        this.stripePaymentGateway = stripePaymentGateway;
        this.alipayPaymentGateway = alipayPaymentGateway;
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

    @PostMapping(value = "/payments/webhook/stripe", consumes = MediaType.ALL_VALUE)
    public Result<Void> stripeWebhook(HttpServletRequest request) throws Exception {
        String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        stripePaymentGateway.resolvePaymentIdFromWebhook(rawBody, readHeaders(request), paymentProperties)
                .ifPresent(paymentId -> subscriptionApplicationService.completePaymentFromGateway(
                        paymentId,
                        "stripe-webhook",
                        "STRIPE"));
        return Result.success(null);
    }

    @PostMapping(value = "/payments/notify/alipay", consumes = MediaType.ALL_VALUE)
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = readParams(request);
        alipayPaymentGateway.resolvePaymentIdFromNotify(params, paymentProperties)
                .ifPresent(paymentId -> subscriptionApplicationService.completePaymentFromGateway(
                        paymentId,
                        params.getOrDefault("trade_no", "alipay-notify"),
                        "ALIPAY"));
        return "success";
    }

    @GetMapping("/invoices")
    public Result<List<BillingInvoiceVO>> invoices() {
        return Result.success(subscriptionApplicationService.listMyInvoices());
    }

    private Map<String, String> readHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private Map<String, String> readParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params.isEmpty() ? Collections.emptyMap() : params;
    }
}
