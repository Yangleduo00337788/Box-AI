package com.boxai.tenant.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.tenant.TenantAccessGuard;
import com.boxai.tenant.api.CreateTenantRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantApplicationServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantAccessGuard tenantAccessGuard;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private PlanApplicationService planApplicationService;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private TenantApplicationService service;

    @Test
    void createForRegistrationBuildsPersonalTenantAndAdminMember() {
        User user = new User();
        user.setId(3L);
        user.setNickname("Ada");
        user.setEmail("ada@example.com");
        when(tenantRepository.findBySlug("ada-personal")).thenReturn(Optional.empty());
        when(tenantRepository.save(any())).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(11L);
            return tenant;
        });

        Tenant tenant = service.createForRegistration(user, "personal", null, null);

        assertEquals(TenantTypes.PERSONAL, tenant.getTenantType());
        assertEquals("Ada 的个人空间", tenant.getName());
        assertEquals("ada-personal", tenant.getSlug());
        assertEquals(3L, tenant.getOwnerId());
        verify(quotaApplicationService).assignDefaultPlan(any(Tenant.class));
        ArgumentCaptor<TenantMember> memberCaptor = ArgumentCaptor.forClass(TenantMember.class);
        verify(tenantRepository).addMember(memberCaptor.capture());
        assertEquals(11L, memberCaptor.getValue().getTenantId());
        assertEquals(RoleCodes.TENANT_ADMIN, memberCaptor.getValue().getRoleCode());
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(new Tenant()));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(
                new CreateTenantRequest("Acme", "ACME", TenantTypes.ENTERPRISE, "ops@acme.com")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void updateStatusRejectsInvalidValue() {
        Tenant tenant = new Tenant();
        tenant.setId(11L);
        when(tenantRepository.findById(11L)).thenReturn(Optional.of(tenant));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStatus(11L, 3));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(tenantRepository, never()).update(any());
    }

    @Test
    void upgradeToEnterpriseRejectsNonPersonalTenant() {
        TenantMember member = new TenantMember();
        member.setTenantId(11L);
        Tenant tenant = new Tenant();
        tenant.setId(11L);
        tenant.setTenantType(TenantTypes.ENTERPRISE);
        when(tenantRepository.findPrimaryByUserId(3L)).thenReturn(Optional.of(member));
        when(tenantRepository.findById(11L)).thenReturn(Optional.of(tenant));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.upgradeToEnterprise(3L, "Acme"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void upgradeToEnterpriseAssignsEnterpriseStarter() {
        TenantMember member = new TenantMember();
        member.setTenantId(11L);
        Tenant tenant = new Tenant();
        tenant.setId(11L);
        tenant.setTenantType(TenantTypes.PERSONAL);
        Plan plan = new Plan();
        plan.setId(22L);
        plan.setName("企业起步");
        when(tenantRepository.findPrimaryByUserId(3L)).thenReturn(Optional.of(member));
        when(tenantRepository.findById(11L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.empty());
        when(planRepository.findByCode("enterprise_starter")).thenReturn(Optional.of(plan));
        when(planApplicationService.requirePlan(22L)).thenReturn(plan);

        var vo = service.upgradeToEnterprise(3L, "  Acme  ");

        assertEquals(TenantTypes.ENTERPRISE, vo.tenantType());
        assertEquals("Acme", vo.name());
        assertEquals(22L, vo.planId());
        assertEquals("企业起步", vo.planName());
        verify(tenantRepository).update(tenant);
    }
}
