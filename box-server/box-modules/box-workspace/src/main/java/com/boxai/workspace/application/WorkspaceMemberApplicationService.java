package com.boxai.workspace.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.constant.RoleCodes;
import com.boxai.security.audit.AuditLogService;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.notification.Notification;
import com.boxai.domain.notification.NotificationRepository;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.workspace.api.InviteWorkspaceMemberRequest;
import com.boxai.workspace.api.UpdateWorkspaceMemberRoleRequest;
import com.boxai.workspace.api.WorkspaceMemberVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class WorkspaceMemberApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public WorkspaceMemberApplicationService(WorkspaceRepository workspaceRepository,
                                             RoleRepository roleRepository,
                                             UserRepository userRepository,
                                             WorkspacePermissionService workspacePermissionService,
                                             NotificationRepository notificationRepository,
                                             AuditLogService auditLogService) {
        this.workspaceRepository = workspaceRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
    }

    public List<WorkspaceMemberVO> listCurrentWorkspaceMembers() {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        return workspaceRepository.listMembersByWorkspaceId(workspaceId).stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public WorkspaceMemberVO invite(InviteWorkspaceMemberRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        User user = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，请先注册"));
        if (workspaceRepository.findMember(workspaceId, user.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户已是工作空间成员");
        }
        Role role = resolveRole(workspaceId, request.roleCode());
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(user.getId());
        member.setRoleId(role.getId());
        member.setStatus(1);
        workspaceRepository.addMember(member);
        member.setRoleCode(role.getRoleCode());
        notifyWorkspaceInvite(workspaceId, user.getId());
        auditLogService.recordSuccess(
                AuditActions.MEMBER_INVITE,
                AuditResourceTypes.MEMBER,
                user.getId(),
                user.getEmail(),
                "role=" + role.getRoleCode());
        return toVo(member);
    }

    private void notifyWorkspaceInvite(Long workspaceId, Long userId) {
        Notification notification = new Notification();
        notification.setWorkspaceId(workspaceId);
        notification.setUserId(userId);
        notification.setTitle("已加入工作空间");
        notification.setContent("你已被加入当前工作空间，可以开始协作开发");
        notification.setCategory("TEAM");
        notification.setLinkUrl("/team");
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public WorkspaceMemberVO updateRole(Long userId, UpdateWorkspaceMemberRoleRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        WorkspaceMember member = workspaceRepository.findMember(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工作空间成员不存在"));
        Role role = resolveRole(workspaceId, request.roleCode());
        member.setRoleId(role.getId());
        member.setRoleCode(role.getRoleCode());
        workspaceRepository.updateMember(member);
        auditLogService.recordSuccess(
                AuditActions.MEMBER_ROLE_UPDATE,
                AuditResourceTypes.MEMBER,
                userId,
                String.valueOf(userId),
                "role=" + role.getRoleCode());
        return toVo(member);
    }

    @Transactional
    public void remove(Long userId) {
        workspacePermissionService.requirePermission(PermissionCodes.MEMBER_MANAGE);
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long operatorId = WorkspaceContext.require().userId();
        if (operatorId.equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能移除当前登录账号");
        }
        workspaceRepository.findMember(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工作空间成员不存在"));
        workspaceRepository.removeMember(workspaceId, userId);
        auditLogService.recordSuccess(
                AuditActions.MEMBER_REMOVE,
                AuditResourceTypes.MEMBER,
                userId,
                String.valueOf(userId),
                null);
    }

    private Role resolveRole(Long workspaceId, String roleCode) {
        String normalized = roleCode == null || roleCode.isBlank() ? RoleCodes.MEMBER : roleCode.trim();
        if (!RoleCodes.TENANT_ADMIN.equals(normalized)
                && !RoleCodes.DEVELOPER.equals(normalized)
                && !RoleCodes.MEMBER.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色无效");
        }
        return roleRepository.findByWorkspaceAndCode(workspaceId, normalized)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工作空间角色不存在"));
    }

    private WorkspaceMemberVO toVo(WorkspaceMember member) {
        User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return new WorkspaceMemberVO(
                member.getId(),
                member.getUserId(),
                user.getEmail(),
                user.getNickname(),
                member.getRoleCode(),
                member.getStatus(),
                null);
    }
}
