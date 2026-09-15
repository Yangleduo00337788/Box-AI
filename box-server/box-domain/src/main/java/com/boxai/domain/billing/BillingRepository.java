package com.boxai.domain.billing;

import java.util.List;
import java.util.Optional;

public interface BillingRepository {

    Subscription saveSubscription(Subscription subscription);

    void updateSubscription(Subscription subscription);

    Optional<Subscription> findActiveSubscription(Long tenantId);

    List<Subscription> listSubscriptionsByTenant(Long tenantId);

    BillingInvoice saveInvoice(BillingInvoice invoice);

    void updateInvoice(BillingInvoice invoice);

    Optional<BillingInvoice> findInvoiceById(Long id);

    List<BillingInvoice> listInvoicesByTenant(Long tenantId);

    List<BillingInvoice> listAllInvoices(int limit);

    PaymentRecord savePayment(PaymentRecord payment);

    void updatePayment(PaymentRecord payment);

    Optional<PaymentRecord> findPaymentById(Long id);
}
