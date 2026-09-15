package com.boxai.workspace.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.rbac.Role;
import com.boxai.domain.rbac.RoleRepository;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceMember;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.tenant.application.QuotaApplicationService;
import com.boxai.workspace.api.CreateWorkspaceRequest;
import com.boxai.workspace.api.UpdateWorkspaceRequest;
import com.boxai.workspace.api.WorkspaceDetailVO;
import com.boxai.workspace.support.RolePermissionSeeder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final QuotaApplicationService quotaApplicationService;
    private final RolePermissionSeeder rolePermissionSeeder;

    public WorkspaceApplicationService(WorkspaceRepository workspaceRepository,
                                       RoleRepository roleRepository,
                                       TenantRepository tenantRepository,
                                       QuotaApplicationService quotaApplicationService,
                                       RolePermissionSeeder rolePermissionSeeder) {
        this.workspaceRepository = workspaceRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.quotaApplicationService = quotaApplicationService;
        this.rolePermissionSeeder = rolePermissionSeeder;
    }

    @Transactional
    public Workspace createDefaultWorkspace(User user, Long tenantId, String tenantType) {
        String workspaceName = TenantTypes.PERSONAL.equals(tenantType)
                ? "我的工作台"
                : user.getNickname() + " 的工作空间";
        String description = TenantTypes.PERSONAL.equals(tenantType)
                ? "个人默认工作台"
                : "企业默认工作空间";
        return createWorkspace(user.getId(), workspaceName, description, null, tenantId);
    }

    @Transactional
    public WorkspaceDetailVO create(LoginUser loginUser, CreateWorkspaceRequest request) {
        Long tenantId = tenantRepository.findPrimaryByUserId(loginUser.userId())
                .map(TenantMember::getTenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到企业租户"));
        quotaApplicationService.assertWorkspaceQuotaAvailable(tenantId);
        Workspace workspace = createWorkspace(loginUser.userId(), request.name(), request.description(), request.avatarUrl(), tenantId);
        return toDetail(workspace, RoleCodes.TENANT_ADMIN);
    }

    @Transactional
    public WorkspaceDetailVO update(LoginUser loginUser, Long workspaceId, UpdateWorkspaceRequest request) {
        requireAdmin(workspaceId, loginUser.userId());
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        workspace.setName(request.name().trim());
        workspace.setDescription(blankToNull(request.description()));
        workspace.setAvatarUrl(blankToNull(request.avatarUrl()));
        if (request.status() != null) {
            workspace.setStatus(request.status() == 0 ? 0 : 1);
        }
        workspaceRepository.update(workspace);
        return requireAccess(workspaceId, loginUser.userId());
    }

    @Transactional
    public void delete(LoginUser loginUser, Long workspaceId) {
        requireAdmin(workspaceId, loginUser.userId());
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        if (workspaceRepository.countByTenantId(workspace.getTenantId()) <= 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除租户下最后一个工作空间");
        }
        workspaceRepository.deleteById(workspaceId);
    }

    public List<WorkspaceDetailVO> listMine(LoginUser loginUser) {
        return listMineByUserId(loginUser.userId());
    }

    @Transactional
    public List<WorkspaceDetailVO> listMineByUserId(Long userId) {
        TenantMember primary = tenantRepository.findPrimaryByUserId(userId).orElse(null);
        if (primary != null
                && primary.getStatus() != null
                && primary.getStatus() == 1
                && RoleCodes.TENANT_ADMIN.equals(primary.getRoleCode())) {
            List<WorkspaceDetailVO> result = new ArrayList<>();
            for (Workspace workspace : workspaceRepository.listByTenantId(primary.getTenantId())) {
                if (workspace.getStatus() == null || workspace.getStatus() != 1) {
                    continue;
                }
                WorkspaceMember member = ensureWorkspaceMember(workspace, userId, RoleCodes.TENANT_ADMIN);
                result.add(toDetail(workspace, member.getRoleCode()));
            }
            return result;
        }
        return workspaceRepository.listMembersByUserId(userId).stream()
                .map(item -> new WorkspaceDetailVO(
                        item.getWorkspaceId(),
                        item.getWorkspaceName(),
                        item.getWorkspaceSlug(),
                        item.getWorkspaceDescription(),
                        item.getWorkspaceAvatarUrl(),
                        item.getWorkspaceStatus(),
                        item.getRoleCode()))
                .toList();
    }

    private Workspace createWorkspace(Long ownerId, String name, String description, String avatarUrl, Long tenantId) {
        Workspace workspace = new Workspace();
        workspace.setTenantId(tenantId);
        workspace.setName(name);
        workspace.setSlug(uniqueSlug(name));
        workspace.setDescription(blankToNull(description));
        workspace.setAvatarUrl(blankToNull(avatarUrl));
        workspace.setOwnerId(ownerId);
        workspace.setStatus(1);
        workspaceRepository.save(workspace);

        Role admin = saveRole(workspace.getId(), RoleCodes.TENANT_ADMIN, "工作空间管理员");
        Role developer = saveRole(workspace.getId(), RoleCodes.DEVELOPER, "开发者");
        Role memberRole = saveRole(workspace.getId(), RoleCodes.MEMBER, "成员");
        rolePermissionSeeder.seedBuiltInRole(admin);
        rolePermissionSeeder.seedBuiltInRole(developer);
        rolePermissionSeeder.seedBuiltInRole(memberRole);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(ownerId);
        member.setRoleId(admin.getId());
        workspaceRepository.addMember(member);
        syncTenantMembersToWorkspace(tenantId, workspace.getId(), ownerId);
        return workspace;
    }

    private Role saveRole(Long workspaceId, String code, String name) {
        Role role = new Role();
        role.setWorkspaceId(workspaceId);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setBuiltIn(1);
        return roleRepository.save(role);
    }

    private void syncTenantMembersToWorkspace(Long tenantId, Long workspaceId, Long ownerId) {
        for (TenantMember tenantMember : tenantRepository.listMembersByTenantId(tenantId)) {
            if (ownerId.equals(tenantMember.getUserId())) {
                continue;
            }
            if (tenantMember.getStatus() == null || tenantMember.getStatus() != 1) {
                continue;
            }
            String roleCode = RoleCodes.TENANT_ADMIN.equals(tenantMember.getRoleCode())
                    ? RoleCodes.TENANT_ADMIN
                    : RoleCodes.MEMBER;
            Workspace workspace = new Workspace();
            workspace.setId(workspaceId);
            ensureWorkspaceMember(workspace, tenantMember.getUserId(), roleCode);
        }
    }

    private WorkspaceMember ensureWorkspaceMember(Workspace workspace, Long userId, String fallbackRoleCode) {
        return workspaceRepository.findMember(workspace.getId(), userId)
                .map(member -> {
                    if (member.getStatus() == null || member.getStatus() != 1) {
                        member.setStatus(1);
                        workspaceRepository.updateMember(member);
                    }
                    return member;
                })
                .orElseGet(() -> {
                    Role role = roleRepository.findByWorkspaceAndCode(workspace.getId(), fallbackRoleCode)
                            .or(() -> roleRepository.findByWorkspaceAndCode(workspace.getId(), RoleCodes.MEMBER))
                            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "工作空间角色未初始化"));
                    WorkspaceMember member = new WorkspaceMember();
                    member.setWorkspaceId(workspace.getId());
                    member.setUserId(userId);
                    member.setRoleId(role.getId());
                    member.setStatus(1);
                    member.setRoleCode(role.getRoleCode());
                    return workspaceRepository.addMember(member);
                });
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "workspace";
        }
        String slug = base;
        int i = 1;
        while (workspaceRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
            if (i > 20) {
                slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return slug;
    }

    public WorkspaceDetailVO requireAccess(Long workspaceId, Long userId) {
        WorkspaceMember member = workspaceRepository.findMember(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工作空间"));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "工作空间不存在"));
        return new WorkspaceDetailVO(workspace.getId(), workspace.getName(), workspace.getSlug(), workspace.getDescription(), workspace.getAvatarUrl(), workspace.getStatus(), member.getRoleCode());
    }

    private void requireAdmin(Long workspaceId, Long userId) {
        WorkspaceMember member = workspaceRepository.findMember(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工作空间"));
        if (!RoleCodes.TENANT_ADMIN.equals(member.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅工作空间管理员可以修改或删除工作空间");
        }
    }

    private WorkspaceDetailVO toDetail(Workspace workspace, String roleCode) {
        return new WorkspaceDetailVO(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getDescription(),
                workspace.getAvatarUrl(),
                workspace.getStatus(),
                roleCode);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
