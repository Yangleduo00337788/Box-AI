package com.boxai.security.permission;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.domain.rbac.RolePermissionQuery;
import com.boxai.security.context.WorkspaceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspacePermissionServiceTest {

    @Mock
    private RolePermissionQuery rolePermissionQuery;

    @InjectMocks
    private WorkspacePermissionService workspacePermissionService;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void tenantAdminBypassesPermissionCheck() {
        WorkspaceContext.set(new WorkspaceContext(1L, 2L, 3L, RoleCodes.TENANT_ADMIN));
        assertDoesNotThrow(() -> workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE));
    }

    @Test
    void memberWithoutPermissionIsRejected() {
        WorkspaceContext.set(new WorkspaceContext(1L, 2L, 3L, RoleCodes.MEMBER));
        when(rolePermissionQuery.hasPermission(3L, PermissionCodes.ROLE_MANAGE)).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> workspacePermissionService.requirePermission(PermissionCodes.ROLE_MANAGE));
    }

    @Test
    void developerWithPermissionIsAllowed() {
        WorkspaceContext.set(new WorkspaceContext(1L, 2L, 3L, RoleCodes.DEVELOPER));
        when(rolePermissionQuery.hasPermission(3L, PermissionCodes.KNOWLEDGE_READ)).thenReturn(true);
        assertDoesNotThrow(() -> workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ));
    }
}
