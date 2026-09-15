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
    private final TenantApplicationService tenantApplicationService;
    private final WorkspaceRepository workspaceRepository;

    public SubscriptionApplicationService(BillingRepository billingRepository,
                                          PlanApplicationService planApplicationService,
                                          TenantRepository tenantRepository,
                                          TenantApplicationService tenantApplicationService,
                                          WorkspaceRepository workspaceRepository) {
        this.billingRepository = billingRepository;
        this.planApplicationService = planApplicationService;
        this.tenantRepository = tenantRepository;
        this.tenantApplicationService = tenantApplicationService;
        this.workspaceRepository = workspaceRepository;
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

        PaymentRecord payment = new PaymentRecord();
        payment.setTenantId(tenantId);
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(amount);
        payment.setCurrency("CNY");
        payment.setChannel("MOCK");
        payment.setStatus("PENDING");
        billingRepository.savePayment(payment);

        return new CreateSubscriptionOrderVO(
                subscription.getId(),
                invoice.getId(),
                payment.getId(),
                invoice.getInvoiceNo(),
                amount,
                "CNY",
                "PENDING");
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
        if ("SUCCEEDED".equals(payment.getStatus())) {
            return toPaymentVo(payment);
        }
        BillingInvoice invoice = billingRepository.findInvoiceById(payment.getInvoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账单不存在"));
        payment.setStatus("SUCCEEDED");
        payment.setPaidAt(LocalDateTime.now());
        payment.setExternalRef("MOCK-" + payment.getId());
        billingRepository.updatePayment(payment);

        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        billingRepository.updateInvoice(invoice);

        Subscription subscription = billingRepository.listSubscriptionsByTenant(tenantId).stream()
                .filter(item -> item.getId().equals(invoice.getSubscriptionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订阅不存在"));
        subscription.setStatus(SubscriptionStatuses.ACTIVE);
        billingRepository.updateSubscription(subscription);

        tenantApplicationService.assignPlan(tenantId, invoice.getPlanId());
        return toPaymentVo(payment);
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
