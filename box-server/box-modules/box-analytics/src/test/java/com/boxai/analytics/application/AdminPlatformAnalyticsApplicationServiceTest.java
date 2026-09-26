package com.boxai.analytics.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.TenantUsage;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPlatformAnalyticsApplicationServiceTest {

    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private TenantUsageRepository tenantUsageRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private AdminPlatformAnalyticsApplicationService service;

    @Test
    void overviewRanksTenantsByMonthlyTokens() {
        when(executionRepository.listRecent(5000)).thenReturn(List.of(
                execution(7L, "SUCCEEDED", 80, 10),
                execution(7L, "FAILED", 40, 0)));
        when(executionRepository.countAll()).thenReturn(10);
        TenantUsage heavy = usage(2L, 1, 900L);
        TenantUsage light = usage(1L, 8, 10L);
        when(tenantUsageRepository.listByPeriod(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(light, heavy));
        Tenant a = tenant(1L, "A");
        Tenant b = tenant(2L, "B");
        when(tenantRepository.listAll()).thenReturn(List.of(a, b));

        var vo = service.overview(0);

        assertEquals(7, vo.periodDays());
        assertEquals(50D, vo.successRate());
        assertEquals(60L, vo.avgLatencyMs());
        assertEquals(10L, vo.periodTokens());
        assertEquals(2, vo.topTenants().size());
        assertEquals(2L, vo.topTenants().get(0).tenantId());
        assertEquals("B", vo.topTenants().get(0).tenantName());
        assertEquals(900L, vo.topTenants().get(0).tokens());
    }

    @Test
    void tenantDetailRejectsMissingTenant() {
        when(tenantRepository.findById(9L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.tenantDetail(9L));
        assertEquals(ErrorCode.TENANT_NOT_FOUND, ex.getCode());
    }

    @Test
    void tenantDetailCountsErrorsForTenantWorkspaces() {
        Tenant tenant = tenant(1L, "Acme");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        Workspace workspace = new Workspace();
        workspace.setId(7L);
        when(workspaceRepository.listByTenantId(1L)).thenReturn(List.of(workspace));
        Execution ok = execution(7L, "SUCCEEDED", 10, 4);
        Execution fail = execution(7L, "FAILED", 20, 0);
        fail.setErrorCode("MODEL_TIMEOUT");
        Execution other = execution(99L, "FAILED", 5, 0);
        when(executionRepository.listRecent(5000)).thenReturn(List.of(ok, fail, other));

        var vo = service.tenantDetail(1L);

        assertEquals("Acme", vo.tenantName());
        assertEquals(2, vo.totalExecutions());
        assertEquals(50D, vo.successRate());
        assertEquals(4L, vo.totalTokens());
        assertEquals(1, vo.modelErrors().size());
        assertEquals("MODEL_TIMEOUT", vo.modelErrors().get(0).modelName());
        assertEquals(1, vo.modelErrors().get(0).errorCount());
        assertEquals(2, vo.modelErrors().get(0).totalCount());
    }

    private static Execution execution(Long workspaceId, String status, long durationMs, int tokens) {
        Execution execution = new Execution();
        execution.setWorkspaceId(workspaceId);
        execution.setStatus(status);
        execution.setDurationMs(durationMs);
        execution.setTotalTokens(tokens);
        execution.setStartedAt(LocalDateTime.now());
        return execution;
    }

    private static Tenant tenant(Long id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        return tenant;
    }

    private static TenantUsage usage(Long tenantId, int calls, long tokens) {
        TenantUsage usage = new TenantUsage();
        usage.setTenantId(tenantId);
        usage.setAiCalls(calls);
        usage.setTokens(tokens);
        return usage;
    }
}
