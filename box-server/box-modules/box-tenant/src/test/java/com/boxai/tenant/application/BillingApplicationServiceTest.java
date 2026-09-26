package com.boxai.tenant.application;

import com.boxai.common.constant.OveragePolicies;
import com.boxai.domain.plan.Plan;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.api.QuotaSnapshotVO;
import com.boxai.tenant.payment.PaymentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingApplicationServiceTest {

    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private PlanApplicationService planApplicationService;
    @Mock
    private PaymentProperties paymentProperties;

    @InjectMocks
    private BillingApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void overviewAddsMeteredOverageToMonthlyPrice() {
        when(quotaApplicationService.getQuotaForWorkspace(7L)).thenReturn(quota(100, 2000L));
        Plan plan = new Plan();
        plan.setPriceMonthly(new BigDecimal("10.00"));
        plan.setOveragePolicy(OveragePolicies.METERED);
        when(planApplicationService.requirePlan(3L)).thenReturn(plan);
        when(paymentProperties.isEnabled()).thenReturn(true);
        when(paymentProperties.isRealGatewayConfigured()).thenReturn(true);

        var vo = service.overview(7L);

        assertEquals(0, vo.estimatedAmount().compareTo(new BigDecimal("11.002")));
        assertEquals(100, vo.overageAiCalls());
        assertEquals(2000L, vo.overageTokens());
        assertTrue(vo.paymentEnabled());
        assertEquals("CNY", vo.currency());
    }

    @Test
    void overviewIgnoresOverageWhenPolicyIsNotMetered() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(quotaApplicationService.getQuotaForWorkspace(7L)).thenReturn(quota(50, 10L));
        Plan plan = new Plan();
        plan.setPriceMonthly(new BigDecimal("8"));
        plan.setOveragePolicy(OveragePolicies.REJECT);
        when(planApplicationService.requirePlan(3L)).thenReturn(plan);
        when(paymentProperties.isEnabled()).thenReturn(false);

        var vo = service.overviewForCurrentWorkspace();

        assertEquals(new BigDecimal("8"), vo.estimatedAmount());
        assertFalse(vo.paymentEnabled());
    }

    private static QuotaSnapshotVO quota(int overageCalls, long overageTokens) {
        return new QuotaSnapshotVO(
                1L, 3L, "Pro", "2026-09",
                1000, 100000L, 5, 3, 2,
                200, 3000L,
                overageCalls, overageTokens,
                OveragePolicies.METERED,
                1, 1, 1,
                800, 97000L, 4, 2, 1);
    }
}
