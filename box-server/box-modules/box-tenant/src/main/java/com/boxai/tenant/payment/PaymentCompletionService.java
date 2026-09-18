package com.boxai.tenant.payment;

import com.boxai.common.constant.SubscriptionStatuses;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.billing.BillingInvoice;
import com.boxai.domain.billing.BillingRepository;
import com.boxai.domain.billing.PaymentRecord;
import com.boxai.domain.billing.Subscription;
import com.boxai.tenant.application.TenantApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentCompletionService {

    private final BillingRepository billingRepository;
    private final TenantApplicationService tenantApplicationService;

    public PaymentCompletionService(BillingRepository billingRepository,
                                    TenantApplicationService tenantApplicationService) {
        this.billingRepository = billingRepository;
        this.tenantApplicationService = tenantApplicationService;
    }

    @Transactional
    public PaymentRecord completePayment(Long paymentId, String externalRef) {
        return completePayment(paymentId, externalRef, null);
    }

    @Transactional
    public PaymentRecord completePayment(Long paymentId, String externalRef, String expectedChannel) {
        PaymentRecord payment = billingRepository.findPaymentById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "支付记录不存在"));
        if ("SUCCEEDED".equals(payment.getStatus())) {
            return payment;
        }
        if (!"PENDING".equals(payment.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付状态无效");
        }
        if (expectedChannel != null && payment.getChannel() != null
                && !expectedChannel.equalsIgnoreCase(payment.getChannel())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付渠道不匹配");
        }
        BillingInvoice invoice = billingRepository.findInvoiceById(payment.getInvoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账单不存在"));
        assertAmountMatches(payment.getAmount(), invoice.getTotalAmount());

        payment.setStatus("SUCCEEDED");
        payment.setPaidAt(LocalDateTime.now());
        payment.setExternalRef(externalRef == null || externalRef.isBlank() ? payment.getExternalRef() : externalRef);
        billingRepository.updatePayment(payment);

        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        billingRepository.updateInvoice(invoice);

        Subscription subscription = billingRepository.listSubscriptionsByTenant(payment.getTenantId()).stream()
                .filter(item -> item.getId().equals(invoice.getSubscriptionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订阅不存在"));
        subscription.setStatus(SubscriptionStatuses.ACTIVE);
        billingRepository.updateSubscription(subscription);

        tenantApplicationService.assignPlan(payment.getTenantId(), invoice.getPlanId());
        return payment;
    }

    private void assertAmountMatches(BigDecimal paymentAmount, BigDecimal invoiceAmount) {
        BigDecimal paid = paymentAmount == null ? BigDecimal.ZERO : paymentAmount;
        BigDecimal expected = invoiceAmount == null ? BigDecimal.ZERO : invoiceAmount;
        if (paid.compareTo(expected) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付金额与账单不一致");
        }
    }
}
