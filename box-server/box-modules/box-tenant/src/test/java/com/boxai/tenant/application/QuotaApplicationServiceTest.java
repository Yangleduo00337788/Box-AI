package com.boxai.tenant.application;

import com.boxai.common.constant.OveragePolicies;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.plan.TenantUsage;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaApplicationServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private TenantUsageRepository tenantUsageRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private PlanApplicationService planApplicationService;

    @InjectMocks
    private QuotaApplicationService service;

    @Test
    void assertMemberQuotaRejectsWhenAtLimit() {
        stubTenantAndPlan(1L, plan(5, 10, 3, 2, 4, OveragePolicies.REJECT));
        when(tenantRepository.listMembersByTenantId(1L)).thenReturn(List.of(
                new TenantMember(), new TenantMember(), new TenantMember()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assertMemberQuotaAvailable(1L));
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.getCode());
    }

    @Test
    void assertWorkspaceQuotaAllowsUnlimitedWhenQuotaIsZero() {
        stubTenantAndPlan(1L, plan(5, 10, 3, 0, 4, OveragePolicies.REJECT));

        service.assertWorkspaceQuotaAvailable(1L);

        verify(workspaceRepository, never()).countByTenantId(1L);
    }

    @Test
    void assertKnowledgeBaseQuotaRejectsWhenAtLimit() {
        stubWorkspaceTenant(8L, 1L);
        stubTenantAndPlan(1L, plan(5, 10, 3, 2, 2, OveragePolicies.REJECT));
        when(knowledgeBaseRepository.countByTenantId(1L)).thenReturn(2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertKnowledgeBaseQuotaAvailable(8L));
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.getCode());
    }

    @Test
    void consumeAiUsageIncrementsWhenWithinLimit() {
        stubWorkspaceTenant(8L, 1L);
        Plan plan = plan(10, 1000, 3, 2, 4, OveragePolicies.REJECT);
        stubTenantAndPlan(1L, plan);
        TenantUsage usage = usage(1L, 1, 20L);
        when(tenantUsageRepository.findByTenantAndPeriod(1L, currentPeriod())).thenReturn(Optional.of(usage));

        service.consumeAiUsage(8L, 15);

        ArgumentCaptor<TenantUsage> captor = ArgumentCaptor.forClass(TenantUsage.class);
        verify(tenantUsageRepository).update(captor.capture());
        assertEquals(2, captor.getValue().getAiCalls());
        assertEquals(35L, captor.getValue().getTokens());
    }

    @Test
    void consumeAiUsageRejectsWhenPolicyIsRejectAndOverLimit() {
        stubWorkspaceTenant(8L, 1L);
        stubTenantAndPlan(1L, plan(1, 1000, 3, 2, 4, OveragePolicies.REJECT));
        when(tenantUsageRepository.findByTenantAndPeriod(1L, currentPeriod()))
                .thenReturn(Optional.of(usage(1L, 1, 10L)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consumeAiUsage(8L, 1));
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.getCode());
        verify(tenantUsageRepository, never()).update(any());
    }

    @Test
    void consumeAiUsageRecordsOverageWhenPolicyIsDegrade() {
        stubWorkspaceTenant(8L, 1L);
        stubTenantAndPlan(1L, plan(1, 1000, 3, 2, 4, OveragePolicies.DEGRADE));
        TenantUsage usage = usage(1L, 1, 10L);
        when(tenantUsageRepository.findByTenantAndPeriod(1L, currentPeriod())).thenReturn(Optional.of(usage));

        service.consumeAiUsage(8L, 8);

        ArgumentCaptor<TenantUsage> captor = ArgumentCaptor.forClass(TenantUsage.class);
        verify(tenantUsageRepository).update(captor.capture());
        assertEquals(1, captor.getValue().getAiCalls());
        assertEquals(10L, captor.getValue().getTokens());
        assertEquals(1, captor.getValue().getOverageAiCalls());
        assertEquals(8L, captor.getValue().getOverageTokens());
    }

    @Test
    void assignDefaultPlanSetsPersonalFree() {
        Tenant tenant = new Tenant();
        tenant.setTenantType("PERSONAL");
        Plan free = new Plan();
        free.setId(11L);
        when(planRepository.findByCode("personal_free")).thenReturn(Optional.of(free));

        service.assignDefaultPlan(tenant);

        assertEquals(11L, tenant.getPlanId());
    }

    private void stubWorkspaceTenant(Long workspaceId, Long tenantId) {
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setTenantId(tenantId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
    }

    private void stubTenantAndPlan(Long tenantId, Plan plan) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlanId(plan.getId());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planApplicationService.requirePlan(plan.getId())).thenReturn(plan);
    }

    private static Plan plan(int aiCalls, long tokens, int members, int workspaces, int kbs, String policy) {
        Plan plan = new Plan();
        plan.setId(99L);
        plan.setName("test");
        plan.setQuotaAiCalls(aiCalls);
        plan.setQuotaTokens(tokens);
        plan.setQuotaMembers(members);
        plan.setQuotaWorkspaces(workspaces);
        plan.setQuotaKnowledgeBases(kbs);
        plan.setOveragePolicy(policy);
        return plan;
    }

    private static TenantUsage usage(Long tenantId, int aiCalls, long tokens) {
        TenantUsage usage = new TenantUsage();
        usage.setTenantId(tenantId);
        usage.setPeriod(currentPeriod());
        usage.setAiCalls(aiCalls);
        usage.setTokens(tokens);
        return usage;
    }

    private static String currentPeriod() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
