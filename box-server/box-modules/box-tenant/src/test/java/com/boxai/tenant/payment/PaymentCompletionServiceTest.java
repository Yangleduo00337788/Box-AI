package com.boxai.tenant.payment;

import com.boxai.common.constant.SubscriptionStatuses;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.billing.BillingInvoice;
import com.boxai.domain.billing.BillingRepository;
import com.boxai.domain.billing.PaymentRecord;
import com.boxai.domain.billing.Subscription;
import com.boxai.tenant.application.TenantApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompletionServiceTest {

    @Mock
    private BillingRepository billingRepository;
    @Mock
    private TenantApplicationService tenantApplicationService;

    @InjectMocks
    private PaymentCompletionService service;

    @Test
    void completePaymentIsIdempotentWhenAlreadySucceeded() {
        PaymentRecord payment = payment("SUCCEEDED", "MOCK", BigDecimal.TEN);
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment));

        PaymentRecord result = service.completePayment(31L, "ext", "MOCK");

        assertSame(payment, result);
        verify(billingRepository, never()).updatePayment(any());
        verify(tenantApplicationService, never()).assignPlan(anyLong(), any());
    }

    @Test
    void completePaymentRejectsNonPendingStatus() {
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment("FAILED", "MOCK", BigDecimal.TEN)));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.completePayment(31L, "ext", "MOCK"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void completePaymentRejectsChannelMismatch() {
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment("PENDING", "STRIPE", BigDecimal.TEN)));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.completePayment(31L, "ext", "MOCK"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void completePaymentRejectsAmountMismatch() {
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment("PENDING", "MOCK", BigDecimal.TEN)));
        BillingInvoice invoice = invoice(new BigDecimal("99.00"));
        when(billingRepository.findInvoiceById(21L)).thenReturn(Optional.of(invoice));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.completePayment(31L, "ext", "MOCK"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void completePaymentActivatesSubscriptionAndAssignsPlan() {
        PaymentRecord payment = payment("PENDING", "MOCK", new BigDecimal("99.00"));
        BillingInvoice invoice = invoice(new BigDecimal("99.00"));
        Subscription subscription = new Subscription();
        subscription.setId(11L);
        subscription.setStatus(SubscriptionStatuses.PENDING_PAYMENT);
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment));
        when(billingRepository.findInvoiceById(21L)).thenReturn(Optional.of(invoice));
        when(billingRepository.listSubscriptionsByTenant(1L)).thenReturn(List.of(subscription));

        PaymentRecord result = service.completePayment(31L, "ext-9", "mock");

        assertEquals("SUCCEEDED", result.getStatus());
        assertEquals("ext-9", result.getExternalRef());
        assertEquals("PAID", invoice.getStatus());
        assertEquals(SubscriptionStatuses.ACTIVE, subscription.getStatus());
        verify(billingRepository).updatePayment(payment);
        verify(billingRepository).updateInvoice(invoice);
        verify(billingRepository).updateSubscription(subscription);
        verify(tenantApplicationService).assignPlan(1L, 9L);
    }

    private static PaymentRecord payment(String status, String channel, BigDecimal amount) {
        PaymentRecord payment = new PaymentRecord();
        payment.setId(31L);
        payment.setTenantId(1L);
        payment.setInvoiceId(21L);
        payment.setStatus(status);
        payment.setChannel(channel);
        payment.setAmount(amount);
        return payment;
    }

    private static BillingInvoice invoice(BigDecimal total) {
        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(21L);
        invoice.setSubscriptionId(11L);
        invoice.setPlanId(9L);
        invoice.setTotalAmount(total);
        return invoice;
    }
}
