package com.boxai.tenant.application;

import com.boxai.domain.plan.Plan;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.api.BillingOverviewVO;
import com.boxai.tenant.api.QuotaSnapshotVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BillingApplicationService {

    private final QuotaApplicationService quotaApplicationService;
    private final PlanApplicationService planApplicationService;

    public BillingApplicationService(QuotaApplicationService quotaApplicationService,
                                     PlanApplicationService planApplicationService) {
        this.quotaApplicationService = quotaApplicationService;
        this.planApplicationService = planApplicationService;
    }

    public BillingOverviewVO overview(Long workspaceId) {
        QuotaSnapshotVO quota = quotaApplicationService.getQuotaForWorkspace(workspaceId);
        Plan plan = planApplicationService.requirePlan(quota.planId());
        BigDecimal monthly = plan.getPriceMonthly() == null ? BigDecimal.ZERO : plan.getPriceMonthly();
        BigDecimal overageAmount = calculateOverageAmount(plan, quota);
        BigDecimal estimated = monthly.add(overageAmount);
        return new BillingOverviewVO(
                quota.period(),
                quota.planName(),
                monthly,
                quota.usedAiCalls(),
                quota.usedTokens(),
                quota.quotaAiCalls(),
                quota.quotaTokens(),
                quota.overageAiCalls() == null ? 0 : quota.overageAiCalls(),
                quota.overageTokens() == null ? 0L : quota.overageTokens(),
                plan.getOveragePolicy(),
                estimated,
                "CNY",
                false);
    }

    private BigDecimal calculateOverageAmount(Plan plan, QuotaSnapshotVO quota) {
        if (!"METERED".equals(plan.getOveragePolicy())) {
            return BigDecimal.ZERO;
        }
        int overageCalls = quota.overageAiCalls() == null ? 0 : quota.overageAiCalls();
        long overageTokens = quota.overageTokens() == null ? 0L : quota.overageTokens();
        if (overageCalls <= 0 && overageTokens <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal callRate = new BigDecimal("0.01");
        BigDecimal tokenRate = new BigDecimal("0.000001");
        return callRate.multiply(BigDecimal.valueOf(overageCalls))
                .add(tokenRate.multiply(BigDecimal.valueOf(overageTokens)));
    }

    public BillingOverviewVO overviewForCurrentWorkspace() {
        return overview(WorkspaceContext.require().workspaceId());
    }
}
