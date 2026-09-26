package com.boxai.workspace.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RolePermissionQuery;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workspace.api.CreateRoleRequest;
import com.boxai.workspace.api.UpdateRoleRequest;
import com.boxai.workspace.support.RolePermissionSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleApplicationServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionQuery rolePermissionQuery;
    @Mock
    private RolePermissionSeeder rolePermissionSeeder;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RoleApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createPersistsCustomRoleAndPermissions() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "TENANT_ADMIN"));
        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(21L);
            return role;
        }).when(roleRepository).save(any(Role.class));
        when(rolePermissionQuery.listPermissionCodes(21L)).thenReturn(List.of("agent:create"));

        var vo = service.create(new CreateRoleRequest("  Reviewer  ", "desc", List.of("agent:create")));

        verify(workspacePermissionService).requirePermission(PermissionCodes.ROLE_MANAGE);
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getWorkspaceId());
        assertEquals("Reviewer", captor.getValue().getRoleName());
        assertEquals(0, captor.getValue().getBuiltIn());
        assertTrue(captor.getValue().getRoleCode().startsWith("CUSTOM_"));
        verify(rolePermissionQuery).replacePermissions(21L, List.of("agent:create"));
        assertEquals("Reviewer", vo.roleName());
        assertFalse(vo.builtIn());
    }

    @Test
    void updateRejectsBuiltInRole() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "TENANT_ADMIN"));
        Role role = new Role();
        role.setId(2L);
        role.setWorkspaceId(7L);
        role.setBuiltIn(1);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(2L, new UpdateRoleRequest("Admin", null, List.of())));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(roleRepository, never()).update(any());
    }

    @Test
    void deleteRejectsRoleFromAnotherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "TENANT_ADMIN"));
        Role role = new Role();
        role.setId(2L);
        role.setWorkspaceId(99L);
        role.setBuiltIn(0);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(2L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(roleRepository, never()).delete(eq(2L));
    }
}
