package com.boxai.tenant.application;

import com.boxai.common.constant.SubscriptionStatuses;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.billing.BillingInvoice;
import com.boxai.domain.billing.BillingRepository;
import com.boxai.domain.billing.PaymentRecord;
import com.boxai.domain.billing.Subscription;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.api.BillingInvoiceVO;
import com.boxai.tenant.api.CreateSubscriptionOrderVO;
import com.boxai.tenant.api.PaymentRecordVO;
import com.boxai.tenant.api.SubscribePlanRequest;
import com.boxai.tenant.payment.PaymentCheckoutCommand;
import com.boxai.tenant.payment.PaymentCheckoutResult;
import com.boxai.tenant.payment.PaymentCompletionService;
import com.boxai.tenant.payment.PaymentGateway;
import com.boxai.tenant.payment.PaymentGatewayRegistry;
import com.boxai.tenant.payment.PaymentProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionApplicationService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BillingRepository billingRepository;
    private final PlanApplicationService planApplicationService;
    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final PaymentProperties paymentProperties;
    private final PaymentCompletionService paymentCompletionService;

    public SubscriptionApplicationService(BillingRepository billingRepository,
                                          PlanApplicationService planApplicationService,
                                          TenantRepository tenantRepository,
                                          TenantApplicationService tenantApplicationService,
                                          WorkspaceRepository workspaceRepository,
                                          PaymentGatewayRegistry paymentGatewayRegistry,
                                          PaymentProperties paymentProperties,
                                          PaymentCompletionService paymentCompletionService) {
        this.billingRepository = billingRepository;
        this.planApplicationService = planApplicationService;
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.paymentProperties = paymentProperties;
        this.paymentCompletionService = paymentCompletionService;
    }

    public List<com.boxai.tenant.api.PlanVO> listPublicPlans() {
        return planApplicationService.listAll().stream()
                .filter(item -> item.status() == 1)
                .toList();
    }

    @Transactional
    public CreateSubscriptionOrderVO subscribe(SubscribePlanRequest request) {
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long tenantId = resolveTenantId(workspaceId);
        Long userId = WorkspaceContext.require().userId();
        Plan plan = planApplicationService.requirePlan(request.planId());
        if (plan.getStatus() == null || plan.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "套餐不可用");
        }
        cancelOpenSubscriptions(tenantId);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        Subscription subscription = new Subscription();
        subscription.setTenantId(tenantId);
        subscription.setPlanId(plan.getId());
        subscription.setStatus(SubscriptionStatuses.PENDING_PAYMENT);
        subscription.setBillingCycle("MONTHLY");
        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(end);
        subscription.setCreatedBy(userId);
        billingRepository.saveSubscription(subscription);

        BigDecimal amount = plan.getPriceMonthly() == null ? BigDecimal.ZERO : plan.getPriceMonthly();
        BillingInvoice invoice = new BillingInvoice();
        invoice.setTenantId(tenantId);
        invoice.setSubscriptionId(subscription.getId());
        invoice.setInvoiceNo("INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        invoice.setPeriod(LocalDate.now().format(PERIOD));
        invoice.setPlanId(plan.getId());
        invoice.setPlanName(plan.getName());
        invoice.setSubtotal(amount);
        invoice.setOverageAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(amount);
        invoice.setCurrency("CNY");
        invoice.setStatus("OPEN");
        billingRepository.saveInvoice(invoice);

        PaymentGateway gateway = paymentGatewayRegistry.resolve(paymentProperties);
        PaymentRecord payment = new PaymentRecord();
        payment.setTenantId(tenantId);
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(amount);
        payment.setCurrency("CNY");
        payment.setChannel(gateway.channel());
        payment.setStatus("PENDING");
        billingRepository.savePayment(payment);

        PaymentCheckoutResult checkout = gateway.createCheckout(
                new PaymentCheckoutCommand(
                        payment.getId(),
                        tenantId,
                        invoice.getId(),
                        invoice.getInvoiceNo(),
                        amount,
                        "CNY",
                        plan.getName()),
                paymentProperties);
        payment.setExternalRef(checkout.externalRef());
        billingRepository.updatePayment(payment);

        return new CreateSubscriptionOrderVO(
                subscription.getId(),
                invoice.getId(),
                payment.getId(),
                invoice.getInvoiceNo(),
                amount,
                "CNY",
                "PENDING",
                checkout.channel(),
                checkout.paymentUrl(),
                checkout.requiresClientConfirm());
    }

    @Transactional
    public PaymentRecordVO confirmPayment(Long paymentId) {
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long tenantId = resolveTenantId(workspaceId);
        PaymentRecord payment = billingRepository.findPaymentById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "支付记录不存在"));
        if (!payment.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该支付");
        }
        if (!"MOCK".equalsIgnoreCase(payment.getChannel())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前支付渠道不支持手动确认");
        }
        if (paymentProperties.isEnabled() && paymentProperties.isRealGatewayConfigured()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已启用真实支付，不支持模拟确认");
        }
        PaymentRecord completed = paymentCompletionService.completePayment(
                paymentId, "MOCK-" + payment.getId(), "MOCK");
        return toPaymentVo(completed);
    }

    @Transactional
    public void completePaymentFromGateway(Long paymentId, String externalRef, String expectedChannel) {
        paymentCompletionService.completePayment(paymentId, externalRef, expectedChannel);
    }

    private void cancelOpenSubscriptions(Long tenantId) {
        for (Subscription subscription : billingRepository.listSubscriptionsByTenant(tenantId)) {
            String status = subscription.getStatus();
            if (!SubscriptionStatuses.ACTIVE.equals(status)
                    && !SubscriptionStatuses.PENDING_PAYMENT.equals(status)) {
                continue;
            }
            subscription.setStatus(SubscriptionStatuses.CANCELLED);
            billingRepository.updateSubscription(subscription);
        }
    }

    public List<BillingInvoiceVO> listMyInvoices() {
        Long tenantId = resolveTenantId(WorkspaceContext.require().workspaceId());
        return billingRepository.listInvoicesByTenant(tenantId).stream().map(this::toInvoiceVo).toList();
    }

    public List<BillingInvoiceVO> listAllInvoicesForAdmin() {
        return billingRepository.listAllInvoices(200).stream().map(this::toInvoiceVo).toList();
    }

    private Long resolveTenantId(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        if (workspace.getTenantId() == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "工作空间未绑定租户");
        }
        return workspace.getTenantId();
    }

    private BillingInvoiceVO toInvoiceVo(BillingInvoice invoice) {
        Tenant tenant = tenantRepository.findById(invoice.getTenantId()).orElse(null);
        return new BillingInvoiceVO(
                invoice.getId(),
                invoice.getTenantId(),
                tenant == null ? null : tenant.getName(),
                invoice.getInvoiceNo(),
                invoice.getPeriod(),
                invoice.getPlanName(),
                invoice.getSubtotal(),
                invoice.getOverageAmount(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getPaidAt(),
                invoice.getCreatedAt());
    }

    private PaymentRecordVO toPaymentVo(PaymentRecord payment) {
        return new PaymentRecordVO(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getChannel(),
                payment.getStatus(),
                payment.getPaidAt());
    }
}
