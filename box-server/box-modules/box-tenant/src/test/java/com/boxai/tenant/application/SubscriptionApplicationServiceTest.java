package com.boxai.tenant.application;

import com.boxai.common.constant.SubscriptionStatuses;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.billing.BillingInvoice;
import com.boxai.domain.billing.BillingRepository;
import com.boxai.domain.billing.PaymentRecord;
import com.boxai.domain.billing.Subscription;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.api.PlanVO;
import com.boxai.tenant.api.SubscribePlanRequest;
import com.boxai.tenant.payment.PaymentCheckoutResult;
import com.boxai.tenant.payment.PaymentCompletionService;
import com.boxai.tenant.payment.PaymentGateway;
import com.boxai.tenant.payment.PaymentGatewayRegistry;
import com.boxai.tenant.payment.PaymentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionApplicationServiceTest {

    @Mock
    private BillingRepository billingRepository;
    @Mock
    private PlanApplicationService planApplicationService;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantApplicationService tenantApplicationService;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private PaymentGatewayRegistry paymentGatewayRegistry;
    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private PaymentCompletionService paymentCompletionService;
    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private SubscriptionApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void listPublicPlansKeepsActivePlansOnly() {
        when(planApplicationService.listAll()).thenReturn(List.of(
                planVo(1L, 1),
                planVo(2L, 0)));
        assertEquals(List.of(1L), service.listPublicPlans().stream().map(PlanVO::id).toList());
    }

    @Test
    void subscribeCreatesPendingOrderAndCancelsOpenSubscriptions() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "OWNER"));
        stubWorkspace(7L, 1L);
        Plan plan = plan(9L, 1, new BigDecimal("99.00"));
        when(planApplicationService.requirePlan(9L)).thenReturn(plan);
        Subscription previous = new Subscription();
        previous.setId(4L);
        previous.setStatus(SubscriptionStatuses.ACTIVE);
        when(billingRepository.listSubscriptionsByTenant(1L)).thenReturn(List.of(previous));
        doAnswer(invocation -> {
            Subscription item = invocation.getArgument(0);
            item.setId(11L);
            return item;
        }).when(billingRepository).saveSubscription(any());
        doAnswer(invocation -> {
            BillingInvoice item = invocation.getArgument(0);
            item.setId(21L);
            return item;
        }).when(billingRepository).saveInvoice(any());
        doAnswer(invocation -> {
            PaymentRecord item = invocation.getArgument(0);
            item.setId(31L);
            return item;
        }).when(billingRepository).savePayment(any());
        when(paymentGatewayRegistry.resolve(paymentProperties)).thenReturn(paymentGateway);
        when(paymentGateway.channel()).thenReturn("MOCK");
        when(paymentGateway.createCheckout(any(), any()))
                .thenReturn(new PaymentCheckoutResult("MOCK", "https://pay.example/c", "ext-1", true));

        var vo = service.subscribe(new SubscribePlanRequest(9L));

        assertEquals(11L, vo.subscriptionId());
        assertEquals(21L, vo.invoiceId());
        assertEquals(31L, vo.paymentId());
        assertEquals(new BigDecimal("99.00"), vo.amount());
        assertEquals("PENDING", vo.paymentStatus());
        assertEquals("MOCK", vo.paymentChannel());
        assertEquals("https://pay.example/c", vo.paymentUrl());
        assertTrue(vo.requiresClientConfirm());
        verify(billingRepository).updateSubscription(previous);
        assertEquals(SubscriptionStatuses.CANCELLED, previous.getStatus());
        ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(billingRepository).updatePayment(paymentCaptor.capture());
        assertEquals("ext-1", paymentCaptor.getValue().getExternalRef());
    }

    @Test
    void subscribeRejectsDisabledPlan() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "OWNER"));
        stubWorkspace(7L, 1L);
        when(planApplicationService.requirePlan(9L)).thenReturn(plan(9L, 0, BigDecimal.TEN));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.subscribe(new SubscribePlanRequest(9L)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(billingRepository, never()).saveSubscription(any());
    }

    @Test
    void confirmPaymentRejectsOtherTenant() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "OWNER"));
        stubWorkspace(7L, 1L);
        PaymentRecord payment = payment(31L, 2L, "MOCK");
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmPayment(31L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void confirmPaymentRejectsNonMockChannel() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "OWNER"));
        stubWorkspace(7L, 1L);
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment(31L, 1L, "STRIPE")));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmPayment(31L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void confirmPaymentCompletesMockPayment() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "OWNER"));
        stubWorkspace(7L, 1L);
        PaymentRecord payment = payment(31L, 1L, "MOCK");
        when(billingRepository.findPaymentById(31L)).thenReturn(Optional.of(payment));
        when(paymentProperties.isEnabled()).thenReturn(false);
        PaymentRecord completed = payment(31L, 1L, "MOCK");
        completed.setStatus("SUCCEEDED");
        completed.setPaidAt(LocalDateTime.now());
        when(paymentCompletionService.completePayment(31L, "MOCK-31", "MOCK")).thenReturn(completed);

        var vo = service.confirmPayment(31L);

        assertEquals("SUCCEEDED", vo.status());
        assertEquals(31L, vo.id());
    }

    private void stubWorkspace(Long workspaceId, Long tenantId) {
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setTenantId(tenantId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
    }

    private static Plan plan(Long id, int status, BigDecimal price) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setName("Pro");
        plan.setStatus(status);
        plan.setPriceMonthly(price);
        return plan;
    }

    private static PlanVO planVo(Long id, int status) {
        return new PlanVO(id, "p", "Plan", null, BigDecimal.ZERO, 1, 1L, 1, 1, 1, "ALL", null, 0, status, null);
    }

    private static PaymentRecord payment(Long id, Long tenantId, String channel) {
        PaymentRecord payment = new PaymentRecord();
        payment.setId(id);
        payment.setTenantId(tenantId);
        payment.setInvoiceId(21L);
        payment.setAmount(BigDecimal.TEN);
        payment.setCurrency("CNY");
        payment.setChannel(channel);
        payment.setStatus("PENDING");
        return payment;
    }
}
