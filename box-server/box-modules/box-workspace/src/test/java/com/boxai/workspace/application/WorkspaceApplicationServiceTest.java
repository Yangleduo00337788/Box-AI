package com.boxai.workspace.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.tenant.application.QuotaApplicationService;
import com.boxai.workspace.api.CreateWorkspaceRequest;
import com.boxai.workspace.support.RolePermissionSeeder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private RolePermissionSeeder rolePermissionSeeder;

    @InjectMocks
    private WorkspaceApplicationService service;

    @Test
    void createChecksQuotaAndSeedsBuiltinRoles() {
        LoginUser loginUser = new LoginUser(3L, "owner", "TENANT_USER");
        TenantMember member = new TenantMember();
        member.setTenantId(1L);
        when(tenantRepository.findPrimaryByUserId(3L)).thenReturn(Optional.of(member));
        when(workspaceRepository.findBySlug(any())).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            workspace.setId(10L);
            return null;
        }).when(workspaceRepository).save(any(Workspace.class));
        AtomicLong roleIds = new AtomicLong(100);
        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(roleIds.getAndIncrement());
            return role;
        }).when(roleRepository).save(any(Role.class));
        when(tenantRepository.listMembersByTenantId(1L)).thenReturn(List.of());

        var vo = service.create(loginUser, new CreateWorkspaceRequest("Team Space", "desc", null));

        verify(quotaApplicationService).assertWorkspaceQuotaAvailable(1L);
        verify(rolePermissionSeeder, org.mockito.Mockito.times(3)).seedBuiltInRole(any(Role.class));
        verify(workspaceRepository).addMember(any(WorkspaceMember.class));
        assertEquals("Team Space", vo.name());
        assertEquals(RoleCodes.TENANT_ADMIN, vo.roleCode());
        assertEquals(10L, vo.id());
    }

    @Test
    void requireAccessRejectsNonMembers() {
        when(workspaceRepository.findMember(8L, 3L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requireAccess(8L, 3L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
    }

    @Test
    void deleteRejectsNonAdmin() {
        LoginUser loginUser = new LoginUser(3L, "member", "TENANT_USER");
        WorkspaceMember member = new WorkspaceMember();
        member.setRoleCode(RoleCodes.MEMBER);
        when(workspaceRepository.findMember(8L, 3L)).thenReturn(Optional.of(member));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(loginUser, 8L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        verify(workspaceRepository, never()).deleteById(8L);
    }

    @Test
    void deleteRejectsLastWorkspaceInTenant() {
        LoginUser loginUser = new LoginUser(3L, "admin", "TENANT_USER");
        WorkspaceMember member = new WorkspaceMember();
        member.setRoleCode(RoleCodes.TENANT_ADMIN);
        when(workspaceRepository.findMember(8L, 3L)).thenReturn(Optional.of(member));
        Workspace workspace = new Workspace();
        workspace.setId(8L);
        workspace.setTenantId(1L);
        when(workspaceRepository.findById(8L)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.countByTenantId(1L)).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(loginUser, 8L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(workspaceRepository, never()).deleteById(8L);
    }
}
