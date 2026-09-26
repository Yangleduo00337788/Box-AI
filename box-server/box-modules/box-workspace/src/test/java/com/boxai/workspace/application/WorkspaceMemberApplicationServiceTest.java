package com.boxai.workspace.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.notification.NotificationRepository;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workspace.api.InviteWorkspaceMemberRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class WorkspaceMemberApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkspaceMemberApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void inviteRejectsExistingMember() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, RoleCodes.TENANT_ADMIN));
        User user = new User();
        user.setId(9L);
        user.setEmail("a@example.com");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(workspaceRepository.findMember(7L, 9L)).thenReturn(Optional.of(new WorkspaceMember()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.invite(new InviteWorkspaceMemberRequest("A@Example.com", RoleCodes.MEMBER)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(workspacePermissionService).requirePermission(PermissionCodes.MEMBER_MANAGE);
        verify(workspaceRepository, never()).addMember(any());
    }

    @Test
    void inviteRejectsUnknownRole() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, RoleCodes.TENANT_ADMIN));
        User user = new User();
        user.setId(9L);
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(workspaceRepository.findMember(7L, 9L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.invite(new InviteWorkspaceMemberRequest("a@example.com", "GUEST")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void removeRejectsSelf() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, RoleCodes.TENANT_ADMIN));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.remove(3L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(workspaceRepository, never()).removeMember(7L, 3L);
    }

    @Test
    void invitePersistsMemberAndNotifies() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, RoleCodes.TENANT_ADMIN));
        User user = new User();
        user.setId(9L);
        user.setEmail("a@example.com");
        user.setNickname("Ann");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(workspaceRepository.findMember(7L, 9L)).thenReturn(Optional.empty());
        Role role = new Role();
        role.setId(4L);
        role.setRoleCode(RoleCodes.MEMBER);
        when(roleRepository.findByWorkspaceAndCode(7L, RoleCodes.MEMBER)).thenReturn(Optional.of(role));

        var vo = service.invite(new InviteWorkspaceMemberRequest("a@example.com", RoleCodes.MEMBER));

        verify(workspaceRepository).addMember(any(WorkspaceMember.class));
        verify(notificationRepository).save(any());
        assertEquals(9L, vo.userId());
        assertEquals(RoleCodes.MEMBER, vo.roleCode());
        assertEquals("a@example.com", vo.email());
    }
}
