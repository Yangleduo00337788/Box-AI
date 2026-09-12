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
        return new BillingOverviewVO(
                quota.period(),
                quota.planName(),
                monthly,
                quota.usedAiCalls(),
                quota.usedTokens(),
                quota.quotaAiCalls(),
                quota.quotaTokens(),
                monthly,
                "CNY");
    }

    public BillingOverviewVO overviewForCurrentWorkspace() {
        return overview(WorkspaceContext.require().workspaceId());
    }
}
