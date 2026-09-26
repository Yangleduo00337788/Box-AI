package com.boxai.tenant.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.tenant.api.AddTenantMemberRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantMemberApplicationServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private TenantMemberApplicationService service;

    @Test
    void addMemberRejectsPersonalTenant() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(TenantTypes.PERSONAL)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMember(1L, new AddTenantMemberRequest("a@example.com", RoleCodes.MEMBER)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(quotaApplicationService, never()).assertMemberQuotaAvailable(1L);
    }

    @Test
    void addMemberRejectsPlatformAdmin() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(TenantTypes.ENTERPRISE)));
        User user = user(9L, UserTypes.PLATFORM_ADMIN);
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMember(1L, new AddTenantMemberRequest("A@Example.com", RoleCodes.MEMBER)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void addMemberRejectsExistingMember() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(TenantTypes.ENTERPRISE)));
        User user = user(9L, UserTypes.TENANT_USER);
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findMember(1L, 9L)).thenReturn(Optional.of(new TenantMember()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMember(1L, new AddTenantMemberRequest("a@example.com", RoleCodes.MEMBER)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void addMemberChecksQuotaAndSyncsWorkspaces() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(TenantTypes.ENTERPRISE)));
        User user = user(9L, UserTypes.TENANT_USER);
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(tenantRepository.findMember(1L, 9L)).thenReturn(Optional.empty());
        Workspace workspace = new Workspace();
        workspace.setId(7L);
        when(workspaceRepository.listByTenantId(1L)).thenReturn(List.of(workspace));
        when(workspaceRepository.findMember(7L, 9L)).thenReturn(Optional.empty());
        Role role = new Role();
        role.setId(4L);
        role.setRoleCode(RoleCodes.MEMBER);
        when(roleRepository.findByWorkspaceAndCode(7L, RoleCodes.MEMBER)).thenReturn(Optional.of(role));

        var vo = service.addMember(1L, new AddTenantMemberRequest("a@example.com", null));

        verify(quotaApplicationService).assertMemberQuotaAvailable(1L);
        verify(tenantRepository).addMember(any(TenantMember.class));
        verify(workspaceRepository).addMember(any(WorkspaceMember.class));
        assertEquals(RoleCodes.MEMBER, vo.roleCode());
        assertEquals(9L, vo.userId());
    }

    @Test
    void updateMyTenantMemberStatusRejectsSelfDisable() {
        TenantMember operator = new TenantMember();
        operator.setTenantId(1L);
        operator.setUserId(3L);
        operator.setRoleCode(RoleCodes.TENANT_ADMIN);
        operator.setStatus(1);
        when(tenantRepository.findPrimaryByUserId(3L)).thenReturn(Optional.of(operator));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyTenantMemberStatus(3L, 3L, 0));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void addMyTenantMemberRejectsNonAdmin() {
        TenantMember operator = new TenantMember();
        operator.setTenantId(1L);
        operator.setUserId(3L);
        operator.setRoleCode(RoleCodes.MEMBER);
        operator.setStatus(1);
        when(tenantRepository.findPrimaryByUserId(3L)).thenReturn(Optional.of(operator));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMyTenantMember(3L, new AddTenantMemberRequest("a@example.com", RoleCodes.MEMBER)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    private static Tenant tenant(String type) {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantType(type);
        return tenant;
    }

    private static User user(Long id, String userType) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        user.setEmail("a@example.com");
        user.setNickname("Ann");
        return user;
    }
}
