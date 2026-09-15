package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.billing.BillingInvoice;
import com.boxai.domain.billing.BillingRepository;
import com.boxai.domain.billing.PaymentRecord;
import com.boxai.domain.billing.Subscription;
import com.boxai.infrastructure.persistence.entity.BillingInvoiceDO;
import com.boxai.infrastructure.persistence.entity.PaymentRecordDO;
import com.boxai.infrastructure.persistence.entity.SubscriptionDO;
import com.boxai.infrastructure.persistence.mapper.BillingInvoiceMapper;
import com.boxai.infrastructure.persistence.mapper.PaymentRecordMapper;
import com.boxai.infrastructure.persistence.mapper.SubscriptionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BillingRepositoryImpl implements BillingRepository {

    private final SubscriptionMapper subscriptionMapper;
    private final BillingInvoiceMapper billingInvoiceMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public BillingRepositoryImpl(SubscriptionMapper subscriptionMapper,
                                 BillingInvoiceMapper billingInvoiceMapper,
                                 PaymentRecordMapper paymentRecordMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.billingInvoiceMapper = billingInvoiceMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    @Override
    public Subscription saveSubscription(Subscription subscription) {
        SubscriptionDO row = toSubscriptionDo(subscription);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.insert(row);
        subscription.setId(row.getId());
        subscription.setCreatedAt(row.getCreatedAt());
        subscription.setUpdatedAt(row.getUpdatedAt());
        return subscription;
    }

    @Override
    public void updateSubscription(Subscription subscription) {
        SubscriptionDO row = toSubscriptionDo(subscription);
        row.setId(subscription.getId());
        row.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.update(row);
        subscription.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Subscription> findActiveSubscription(Long tenantId) {
        return subscriptionMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("tenant_id", tenantId)
                                .eq("status", "ACTIVE")
                                .orderBy("id", false)
                                .limit(1))
                .stream()
                .findFirst()
                .map(this::toSubscription);
    }

    @Override
    public List<Subscription> listSubscriptionsByTenant(Long tenantId) {
        return subscriptionMapper.selectListByQuery(
                        QueryWrapper.create().eq("tenant_id", tenantId).orderBy("id", false))
                .stream()
                .map(this::toSubscription)
                .toList();
    }

    @Override
    public BillingInvoice saveInvoice(BillingInvoice invoice) {
        BillingInvoiceDO row = toInvoiceDo(invoice);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        billingInvoiceMapper.insert(row);
        invoice.setId(row.getId());
        invoice.setCreatedAt(row.getCreatedAt());
        invoice.setUpdatedAt(row.getUpdatedAt());
        return invoice;
    }

    @Override
    public void updateInvoice(BillingInvoice invoice) {
        BillingInvoiceDO row = toInvoiceDo(invoice);
        row.setId(invoice.getId());
        row.setUpdatedAt(LocalDateTime.now());
        billingInvoiceMapper.update(row);
        invoice.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<BillingInvoice> findInvoiceById(Long id) {
        return Optional.ofNullable(billingInvoiceMapper.selectOneById(id)).map(this::toInvoice);
    }

    @Override
    public List<BillingInvoice> listInvoicesByTenant(Long tenantId) {
        return billingInvoiceMapper.selectListByQuery(
                        QueryWrapper.create().eq("tenant_id", tenantId).orderBy("id", false))
                .stream()
                .map(this::toInvoice)
                .toList();
    }

    @Override
    public List<BillingInvoice> listAllInvoices(int limit) {
        return billingInvoiceMapper.selectListByQuery(
                        QueryWrapper.create().orderBy("id", false).limit(limit))
                .stream()
                .map(this::toInvoice)
                .toList();
    }

    @Override
    public PaymentRecord savePayment(PaymentRecord payment) {
        PaymentRecordDO row = toPaymentDo(payment);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(row);
        payment.setId(row.getId());
        payment.setCreatedAt(row.getCreatedAt());
        payment.setUpdatedAt(row.getUpdatedAt());
        return payment;
    }

    @Override
    public void updatePayment(PaymentRecord payment) {
        PaymentRecordDO row = toPaymentDo(payment);
        row.setId(payment.getId());
        row.setUpdatedAt(LocalDateTime.now());
        paymentRecordMapper.update(row);
        payment.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<PaymentRecord> findPaymentById(Long id) {
        return Optional.ofNullable(paymentRecordMapper.selectOneById(id)).map(this::toPayment);
    }

    private Subscription toSubscription(SubscriptionDO row) {
        Subscription subscription = new Subscription();
        subscription.setId(row.getId());
        subscription.setTenantId(row.getTenantId());
        subscription.setPlanId(row.getPlanId());
        subscription.setStatus(row.getStatus());
        subscription.setBillingCycle(row.getBillingCycle());
        subscription.setCurrentPeriodStart(row.getCurrentPeriodStart());
        subscription.setCurrentPeriodEnd(row.getCurrentPeriodEnd());
        subscription.setCreatedBy(row.getCreatedBy());
        subscription.setCreatedAt(row.getCreatedAt());
        subscription.setUpdatedAt(row.getUpdatedAt());
        return subscription;
    }

    private SubscriptionDO toSubscriptionDo(Subscription subscription) {
        SubscriptionDO row = new SubscriptionDO();
        row.setTenantId(subscription.getTenantId());
        row.setPlanId(subscription.getPlanId());
        row.setStatus(subscription.getStatus());
        row.setBillingCycle(subscription.getBillingCycle());
        row.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        row.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        row.setCreatedBy(subscription.getCreatedBy());
        return row;
    }

    private BillingInvoice toInvoice(BillingInvoiceDO row) {
        BillingInvoice invoice = new BillingInvoice();
        invoice.setId(row.getId());
        invoice.setTenantId(row.getTenantId());
        invoice.setSubscriptionId(row.getSubscriptionId());
        invoice.setInvoiceNo(row.getInvoiceNo());
        invoice.setPeriod(row.getPeriod());
        invoice.setPlanId(row.getPlanId());
        invoice.setPlanName(row.getPlanName());
        invoice.setSubtotal(row.getSubtotal());
        invoice.setOverageAmount(row.getOverageAmount());
        invoice.setTotalAmount(row.getTotalAmount());
        invoice.setCurrency(row.getCurrency());
        invoice.setStatus(row.getStatus());
        invoice.setPaidAt(row.getPaidAt());
        invoice.setCreatedAt(row.getCreatedAt());
        invoice.setUpdatedAt(row.getUpdatedAt());
        return invoice;
    }

    private BillingInvoiceDO toInvoiceDo(BillingInvoice invoice) {
        BillingInvoiceDO row = new BillingInvoiceDO();
        row.setTenantId(invoice.getTenantId());
        row.setSubscriptionId(invoice.getSubscriptionId());
        row.setInvoiceNo(invoice.getInvoiceNo());
        row.setPeriod(invoice.getPeriod());
        row.setPlanId(invoice.getPlanId());
        row.setPlanName(invoice.getPlanName());
        row.setSubtotal(invoice.getSubtotal());
        row.setOverageAmount(invoice.getOverageAmount());
        row.setTotalAmount(invoice.getTotalAmount());
        row.setCurrency(invoice.getCurrency());
        row.setStatus(invoice.getStatus());
        row.setPaidAt(invoice.getPaidAt());
        return row;
    }

    private PaymentRecord toPayment(PaymentRecordDO row) {
        PaymentRecord payment = new PaymentRecord();
        payment.setId(row.getId());
        payment.setTenantId(row.getTenantId());
        payment.setInvoiceId(row.getInvoiceId());
        payment.setAmount(row.getAmount());
        payment.setCurrency(row.getCurrency());
        payment.setChannel(row.getChannel());
        payment.setStatus(row.getStatus());
        payment.setExternalRef(row.getExternalRef());
        payment.setPaidAt(row.getPaidAt());
        payment.setCreatedAt(row.getCreatedAt());
        payment.setUpdatedAt(row.getUpdatedAt());
        return payment;
    }

    private PaymentRecordDO toPaymentDo(PaymentRecord payment) {
        PaymentRecordDO row = new PaymentRecordDO();
        row.setTenantId(payment.getTenantId());
        row.setInvoiceId(payment.getInvoiceId());
        row.setAmount(payment.getAmount());
        row.setCurrency(payment.getCurrency());
        row.setChannel(payment.getChannel());
        row.setStatus(payment.getStatus());
        row.setExternalRef(payment.getExternalRef());
        row.setPaidAt(payment.getPaidAt());
        return row;
    }
}
