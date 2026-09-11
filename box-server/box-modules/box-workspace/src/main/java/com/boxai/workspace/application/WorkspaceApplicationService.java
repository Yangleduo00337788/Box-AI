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
import com.boxai.workspace.api.WorkspaceDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final QuotaApplicationService quotaApplicationService;

    public WorkspaceApplicationService(WorkspaceRepository workspaceRepository,
                                       RoleRepository roleRepository,
                                       TenantRepository tenantRepository,
                                       QuotaApplicationService quotaApplicationService) {
        this.workspaceRepository = workspaceRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.quotaApplicationService = quotaApplicationService;
    }

    @Transactional
    public Workspace createDefaultWorkspace(User user, Long tenantId, String tenantType) {
        String workspaceName = TenantTypes.PERSONAL.equals(tenantType)
                ? "我的工作台"
                : user.getNickname() + " 的工作空间";
        String description = TenantTypes.PERSONAL.equals(tenantType)
                ? "个人默认工作台"
                : "企业默认工作空间";
        return createWorkspace(user.getId(), workspaceName, description, tenantId);
    }

    @Transactional
    public WorkspaceDetailVO create(LoginUser loginUser, CreateWorkspaceRequest request) {
        Long tenantId = tenantRepository.findPrimaryByUserId(loginUser.userId())
                .map(TenantMember::getTenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到企业租户"));
        quotaApplicationService.assertWorkspaceQuotaAvailable(tenantId);
        Workspace workspace = createWorkspace(loginUser.userId(), request.name(), request.description(), tenantId);
        return new WorkspaceDetailVO(workspace.getId(), workspace.getName(), workspace.getSlug(), workspace.getDescription(), RoleCodes.TENANT_ADMIN);
    }

    public List<WorkspaceDetailVO> listMine(LoginUser loginUser) {
        return workspaceRepository.listMembersByUserId(loginUser.userId()).stream()
                .map(item -> new WorkspaceDetailVO(
                        item.getWorkspaceId(),
                        item.getWorkspaceName(),
                        item.getWorkspaceSlug(),
                        null,
                        item.getRoleCode()))
                .toList();
    }

    private Workspace createWorkspace(Long ownerId, String name, String description, Long tenantId) {
        Workspace workspace = new Workspace();
        workspace.setTenantId(tenantId);
        workspace.setName(name);
        workspace.setSlug(uniqueSlug(name));
        workspace.setDescription(description);
        workspace.setOwnerId(ownerId);
        workspace.setStatus(1);
        workspaceRepository.save(workspace);

        Role admin = saveRole(workspace.getId(), RoleCodes.TENANT_ADMIN, "工作空间管理员");
        saveRole(workspace.getId(), RoleCodes.DEVELOPER, "开发者");
        saveRole(workspace.getId(), RoleCodes.MEMBER, "成员");

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(ownerId);
        member.setRoleId(admin.getId());
        workspaceRepository.addMember(member);
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
        return new WorkspaceDetailVO(workspace.getId(), workspace.getName(), workspace.getSlug(), workspace.getDescription(), member.getRoleCode());
    }
}
