package com.boxai.workspace.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.RoleCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.invitation.WorkspaceInvitation;
import com.boxai.domain.invitation.WorkspaceInvitationRepository;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workspace.api.InviteWorkspaceMemberRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceInvitationRepository invitationRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceMemberApplicationService workspaceMemberApplicationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkspaceInvitationApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createInviteSavesPendingWhenUserIsNotRegistered() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, RoleCodes.TENANT_ADMIN));
        Workspace workspace = new Workspace();
        workspace.setId(7L);
        workspace.setTenantId(1L);
        when(workspaceRepository.findById(7L)).thenReturn(Optional.of(workspace));
        Role role = new Role();
        role.setRoleCode(RoleCodes.MEMBER);
        when(roleRepository.findByWorkspaceAndCode(7L, RoleCodes.MEMBER)).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        var vo = service.createInvite(new InviteWorkspaceMemberRequest("New@Example.com", null));

        ArgumentCaptor<WorkspaceInvitation> captor = ArgumentCaptor.forClass(WorkspaceInvitation.class);
        verify(invitationRepository).save(captor.capture());
        verify(workspacePermissionService).requirePermission(PermissionCodes.MEMBER_MANAGE);
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals("new@example.com", captor.getValue().getEmail());
        assertEquals(RoleCodes.MEMBER, vo.roleCode());
    }

    @Test
    void createInviteRejectsExistingMember() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, RoleCodes.TENANT_ADMIN));
        Workspace workspace = new Workspace();
        workspace.setId(7L);
        when(workspaceRepository.findById(7L)).thenReturn(Optional.of(workspace));
        Role role = new Role();
        role.setRoleCode(RoleCodes.MEMBER);
        when(roleRepository.findByWorkspaceAndCode(7L, RoleCodes.MEMBER)).thenReturn(Optional.of(role));
        User user = new User();
        user.setId(9L);
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(workspaceRepository.findMember(7L, 9L)).thenReturn(Optional.of(new WorkspaceMember()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createInvite(new InviteWorkspaceMemberRequest("a@example.com", RoleCodes.MEMBER)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(invitationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptRejectsEmailMismatch() {
        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setEmail("invite@example.com");
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(invitationRepository.findByToken("tok")).thenReturn(Optional.of(invitation));
        User user = new User();
        user.setId(9L);
        user.setEmail("other@example.com");
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.accept("tok", 9L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }
}
