package com.boxai.workspace.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
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
import com.boxai.workspace.api.WorkspaceInvitationVO;
import com.boxai.workspace.api.WorkspaceMemberVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceInvitationApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberApplicationService workspaceMemberApplicationService;
    private final AuditLogService auditLogService;

    public WorkspaceInvitationApplicationService(WorkspaceRepository workspaceRepository,
                                                 WorkspaceInvitationRepository invitationRepository,
                                                 WorkspacePermissionService workspacePermissionService,
                                                 RoleRepository roleRepository,
                                                 UserRepository userRepository,
                                                 WorkspaceMemberApplicationService workspaceMemberApplicationService,
                                                 AuditLogService auditLogService) {
        this.workspaceRepository = workspaceRepository;
        this.invitationRepository = invitationRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.workspaceMemberApplicationService = workspaceMemberApplicationService;
        this.auditLogService = auditLogService;
    }

    public List<WorkspaceInvitationVO> listPending() {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        return invitationRepository.listPendingByWorkspace(workspaceId).stream().map(this::toVo).toList();
    }

    @Transactional
    public WorkspaceInvitationVO createInvite(InviteWorkspaceMemberRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long operatorId = WorkspaceContext.require().userId();
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String roleCode = request.roleCode() == null || request.roleCode().isBlank()
                ? RoleCodes.MEMBER
                : request.roleCode().trim();
        resolveRole(workspaceId, roleCode);

        var existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()
                && workspaceRepository.findMember(workspaceId, existingUser.get().getId()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户已是工作空间成员");
        }
        if (existingUser.isPresent()) {
            workspaceMemberApplicationService.invite(request);
            WorkspaceInvitation accepted = new WorkspaceInvitation();
            accepted.setEmail(email);
            accepted.setRoleCode(roleCode);
            accepted.setStatus("ACCEPTED");
            accepted.setAcceptedAt(LocalDateTime.now());
            return toVo(accepted);
        }

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspaceId(workspaceId);
        invitation.setTenantId(workspace.getTenantId());
        invitation.setEmail(email);
        invitation.setRoleCode(roleCode);
        invitation.setToken(UUID.randomUUID().toString().replace("-", ""));
        invitation.setStatus("PENDING");
        invitation.setInvitedBy(operatorId);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitationRepository.save(invitation);
        auditLogService.recordSuccess(
                AuditActions.MEMBER_INVITE,
                AuditResourceTypes.MEMBER,
                invitation.getId(),
                email,
                "invitation=pending");
        return toVo(invitation);
    }

    public WorkspaceInvitationVO preview(String token) {
        WorkspaceInvitation invitation = requirePendingInvitation(token);
        return toVo(invitation);
    }

    @Transactional
    public WorkspaceMemberVO accept(String token, Long userId) {
        WorkspaceInvitation invitation = requirePendingInvitation(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "邀请邮箱与当前账号不匹配");
        }
        if (workspaceRepository.findMember(invitation.getWorkspaceId(), userId).isPresent()) {
            invitation.setStatus("ACCEPTED");
            invitation.setAcceptedAt(LocalDateTime.now());
            invitation.setAcceptedUserId(userId);
            invitationRepository.update(invitation);
            return workspaceMemberApplicationService.listCurrentWorkspaceMembers().stream()
                    .filter(item -> item.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "成员不存在"));
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(invitation.getWorkspaceId());
        member.setUserId(userId);
        Role role = resolveRole(invitation.getWorkspaceId(), invitation.getRoleCode());
        member.setRoleId(role.getId());
        member.setStatus(1);
        workspaceRepository.addMember(member);

        invitation.setStatus("ACCEPTED");
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setAcceptedUserId(userId);
        invitationRepository.update(invitation);
        return workspaceMemberApplicationService.listCurrentWorkspaceMembers().stream()
                .filter(item -> item.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "成员不存在"));
    }

    @Transactional
    public void revoke(Long invitationId) {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        WorkspaceInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邀请不存在"));
        if (!invitation.getWorkspaceId().equals(workspaceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该邀请");
        }
        invitation.setStatus("REVOKED");
        invitationRepository.update(invitation);
    }

    private WorkspaceInvitation requirePendingInvitation(String token) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邀请不存在"));
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邀请已失效");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.update(invitation);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邀请已过期");
        }
        return invitation;
    }

    private Role resolveRole(Long workspaceId, String roleCode) {
        String normalized = roleCode == null || roleCode.isBlank() ? RoleCodes.MEMBER : roleCode.trim();
        return roleRepository.findByWorkspaceAndCode(workspaceId, normalized)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工作空间角色不存在"));
    }

    private WorkspaceInvitationVO toVo(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationVO(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRoleCode(),
                invitation.getToken(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt());
    }
}
