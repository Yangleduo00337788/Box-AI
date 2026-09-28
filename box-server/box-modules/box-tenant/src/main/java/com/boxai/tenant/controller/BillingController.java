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
import com.boxai.tenant.payment.PaymentUrls;
import com.boxai.tenant.payment.StripePaymentGateway;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

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

    @GetMapping("/payments/return/alipay")
    public void alipayReturn(@RequestParam(required = false) Long paymentId,
                             @RequestParam(value = "out_trade_no", required = false) String outTradeNo,
                             HttpServletResponse response) throws IOException {
        Long id = paymentId;
        if (id == null) {
            id = AlipayPaymentGateway.parsePaymentId(outTradeNo, null).orElse(null);
        }
        if (id != null) {
            try {
                subscriptionApplicationService.syncPaymentFromGateway(id);
            } catch (Exception e) {
                log.warn("支付宝回跳查单失败 paymentId={}: {}", id, e.getMessage());
            }
        }
        String target = paymentProperties.getSuccessUrl();
        if (id != null) {
            target = PaymentUrls.withQuery(target, "paymentId", String.valueOf(id));
        }
        response.sendRedirect(target);
    }

    @GetMapping("/payments/{paymentId}")
    public Result<PaymentRecordVO> payment(@PathVariable Long paymentId) {
        return Result.success(subscriptionApplicationService.getMyPayment(paymentId));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public Result<PaymentRecordVO> confirmPayment(@PathVariable Long paymentId) {
        return Result.success(subscriptionApplicationService.confirmPayment(paymentId));
    }

    @PostMapping(value = "/payments/webhook/stripe", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Result<Void>> stripeWebhook(HttpServletRequest request) throws Exception {
        String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        Map<String, String> headers = readHeaders(request);
        if (!stripePaymentGateway.isWebhookAuthentic(rawBody, headers, paymentProperties)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.failure(400, "Stripe 签名无效"));
        }
        Optional<Long> paymentId = stripePaymentGateway.resolvePaymentIdFromWebhook(rawBody, headers, paymentProperties);
        if (paymentId.isPresent()) {
            try {
                subscriptionApplicationService.completePaymentFromGateway(paymentId.get(), null, "STRIPE");
            } catch (Exception e) {
                log.warn("Stripe 回调入账失败 paymentId={}: {}", paymentId.get(), e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.failure(400, "支付入账失败"));
            }
        }
        return ResponseEntity.ok(Result.success(null));
    }

    @PostMapping(value = "/payments/notify/alipay", consumes = MediaType.ALL_VALUE)
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = readParams(request);
        try {
            Optional<Long> paymentId = alipayPaymentGateway.resolvePaymentIdFromNotify(params, paymentProperties);
            if (paymentId.isPresent()) {
                subscriptionApplicationService.completePaymentFromGateway(
                        paymentId.get(),
                        params.getOrDefault("trade_no", "alipay-notify"),
                        "ALIPAY");
                return "success";
            }
            if (alipayPaymentGateway.isVerifiedNonPaidNotify(params, paymentProperties)) {
                return "success";
            }
            return "fail";
        } catch (Exception e) {
            log.warn("支付宝异步通知处理失败: {}", e.getMessage());
            return "fail";
        }
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
