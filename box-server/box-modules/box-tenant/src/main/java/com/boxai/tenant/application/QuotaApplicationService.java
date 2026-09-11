package com.boxai.tenant.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.plan.TenantUsage;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.tenant.api.QuotaSnapshotVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class QuotaApplicationService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final TenantUsageRepository tenantUsageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PlanApplicationService planApplicationService;

    public QuotaApplicationService(TenantRepository tenantRepository,
                                   PlanRepository planRepository,
                                   TenantUsageRepository tenantUsageRepository,
                                   WorkspaceRepository workspaceRepository,
                                   PlanApplicationService planApplicationService) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.tenantUsageRepository = tenantUsageRepository;
        this.workspaceRepository = workspaceRepository;
        this.planApplicationService = planApplicationService;
    }

    public QuotaSnapshotVO getQuotaForUser(Long userId) {
        Long tenantId = tenantRepository.findPrimaryByUserId(userId)
                .map(member -> member.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
        return buildSnapshot(tenantId);
    }

    public QuotaSnapshotVO getQuotaForTenant(Long tenantId) {
        requireTenant(tenantId);
        return buildSnapshot(tenantId);
    }

    public void assertAiQuotaAvailable(Long workspaceId) {
        Long tenantId = resolveTenantId(workspaceId);
        Plan plan = resolvePlan(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId, currentPeriod());
        assertAiWithinLimit(plan, usage);
    }

    @Transactional
    public void consumeAiUsage(Long workspaceId, long tokens) {
        Long tenantId = resolveTenantId(workspaceId);
        Plan plan = resolvePlan(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId, currentPeriod());
        assertAiWithinLimit(plan, usage);
        usage.setAiCalls(usage.getAiCalls() + 1);
        usage.setTokens(usage.getTokens() + Math.max(tokens, 0));
        tenantUsageRepository.update(usage);
    }

    public void assertMemberQuotaAvailable(Long tenantId) {
        Plan plan = resolvePlan(tenantId);
        if (plan.getQuotaMembers() == null || plan.getQuotaMembers() <= 0) {
            return;
        }
        int used = tenantRepository.listMembersByTenantId(tenantId).size();
        if (used >= plan.getQuotaMembers()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED, "成员数已达套餐上限");
        }
    }

    public void assertWorkspaceQuotaAvailable(Long tenantId) {
        Plan plan = resolvePlan(tenantId);
        if (plan.getQuotaWorkspaces() == null || plan.getQuotaWorkspaces() <= 0) {
            return;
        }
        int used = workspaceRepository.countByTenantId(tenantId);
        if (used >= plan.getQuotaWorkspaces()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED, "工作空间数已达套餐上限");
        }
    }

    public void assignDefaultPlan(Tenant tenant) {
        if (tenant.getPlanId() != null) {
            return;
        }
        String code = "ENTERPRISE".equals(tenant.getTenantType()) ? "enterprise_starter" : "personal_free";
        Plan plan = planRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND, "默认套餐不存在"));
        tenant.setPlanId(plan.getId());
    }

    private QuotaSnapshotVO buildSnapshot(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        Plan plan = resolvePlan(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId, currentPeriod());
        int usedMembers = tenantRepository.listMembersByTenantId(tenantId).size();
        int usedWorkspaces = workspaceRepository.countByTenantId(tenantId);
        return new QuotaSnapshotVO(
                tenantId,
                plan.getId(),
                plan.getName(),
                usage.getPeriod(),
                plan.getQuotaAiCalls(),
                plan.getQuotaTokens(),
                plan.getQuotaMembers(),
                plan.getQuotaWorkspaces(),
                usage.getAiCalls(),
                usage.getTokens(),
                usedMembers,
                usedWorkspaces,
                remaining(plan.getQuotaAiCalls(), usage.getAiCalls()),
                remainingLong(plan.getQuotaTokens(), usage.getTokens()),
                remaining(plan.getQuotaMembers(), usedMembers),
                remaining(plan.getQuotaWorkspaces(), usedWorkspaces));
    }

    private void assertAiWithinLimit(Plan plan, TenantUsage usage) {
        if (plan.getQuotaAiCalls() != null && plan.getQuotaAiCalls() > 0
                && usage.getAiCalls() >= plan.getQuotaAiCalls()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED, "本月 AI 调用次数已达套餐上限");
        }
        if (plan.getQuotaTokens() != null && plan.getQuotaTokens() > 0
                && usage.getTokens() >= plan.getQuotaTokens()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED, "本月 Token 用量已达套餐上限");
        }
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
    }

    private Long resolveTenantId(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        if (workspace.getTenantId() == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "工作空间未绑定租户");
        }
        return workspace.getTenantId();
    }

    private Plan resolvePlan(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getPlanId() == null) {
            assignDefaultPlan(tenant);
            tenantRepository.update(tenant);
        }
        return planApplicationService.requirePlan(tenant.getPlanId());
    }

    private TenantUsage getOrCreateUsage(Long tenantId, String period) {
        return tenantUsageRepository.findByTenantAndPeriod(tenantId, period)
                .orElseGet(() -> {
                    TenantUsage usage = new TenantUsage();
                    usage.setTenantId(tenantId);
                    usage.setPeriod(period);
                    usage.setAiCalls(0);
                    usage.setTokens(0L);
                    return tenantUsageRepository.save(usage);
                });
    }

    private String currentPeriod() {
        return LocalDate.now().format(PERIOD_FORMAT);
    }

    private Integer remaining(Integer quota, int used) {
        if (quota == null || quota <= 0) {
            return null;
        }
        return Math.max(quota - used, 0);
    }

    private Long remainingLong(Long quota, long used) {
        if (quota == null || quota <= 0) {
            return null;
        }
        return Math.max(quota - used, 0L);
    }
}
