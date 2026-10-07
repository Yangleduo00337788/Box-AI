package com.boxai.tenant.application;

import com.boxai.common.constant.RoleCodes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.EnterpriseAccounts;
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
import com.boxai.tenant.api.ProvisionEmployeeRequest;
import com.boxai.tenant.api.TenantMemberVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantMemberApplicationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final QuotaApplicationService quotaApplicationService;
    private final WorkspaceRepository workspaceRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantMemberApplicationService(TenantRepository tenantRepository,
                                          UserRepository userRepository,
                                          QuotaApplicationService quotaApplicationService,
                                          WorkspaceRepository workspaceRepository,
                                          RoleRepository roleRepository,
                                          PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.quotaApplicationService = quotaApplicationService;
        this.workspaceRepository = workspaceRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<TenantMemberVO> listMembers(Long tenantId) {
        requireTenant(tenantId);
        return tenantRepository.listMembersByTenantId(tenantId).stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public TenantMemberVO addMember(Long tenantId, AddTenantMemberRequest request) {
        Tenant tenant = requireTenant(tenantId);
        if (TenantTypes.PERSONAL.equals(tenant.getTenantType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "个人租户不支持添加成员");
        }
        User user = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，请先注册"));
        if (UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能添加平台管理员为租户成员");
        }
        if (tenantRepository.findMember(tenantId, user.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该用户已是租户成员");
        }
        quotaApplicationService.assertMemberQuotaAvailable(tenantId);
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(user.getId());
        member.setRoleCode(normalizeRoleCode(request.roleCode()));
        member.setLoginName(user.getEmail() == null ? user.getUsername() : user.getEmail().trim().toLowerCase(Locale.ROOT));
        member.setStatus(1);
        tenantRepository.addMember(member);
        syncToTenantWorkspaces(tenantId, user.getId(), member.getRoleCode());
        return toVo(member);
    }

    @Transactional
    public void ensureOAuthEnterpriseMember(Long tenantId, Long userId, boolean admin) {
        if (tenantRepository.findMember(tenantId, userId).isPresent()) {
            return;
        }
        quotaApplicationService.assertMemberQuotaAvailable(tenantId);
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(userId);
        member.setRoleCode(admin ? RoleCodes.TENANT_ADMIN : RoleCodes.MEMBER);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        member.setLoginName(user.getEmail() == null ? user.getUsername() : user.getEmail().trim().toLowerCase(Locale.ROOT));
        member.setStatus(1);
        tenantRepository.addMember(member);
        syncToTenantWorkspaces(tenantId, userId, member.getRoleCode());
    }

    @Transactional
    public TenantMemberVO updateMemberStatus(Long tenantId, Long userId, Integer status) {
        requireTenant(tenantId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值无效");
        }
        TenantMember member = tenantRepository.findMember(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "租户成员不存在"));
        member.setStatus(status);
        tenantRepository.updateMember(member);
        return toVo(member);
    }

    public List<TenantMemberVO> listMyTenantMembers(Long operatorUserId) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        return listMembers(operator.getTenantId());
    }

    @Transactional
    public TenantMemberVO addMyTenantMember(Long operatorUserId, AddTenantMemberRequest request) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        return addMember(operator.getTenantId(), request);
    }

    @Transactional
    public TenantMemberVO updateMyTenantMemberStatus(Long operatorUserId, Long userId, Integer status) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        if (operator.getUserId().equals(userId) && status != null && status == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能停用当前登录账号");
        }
        return updateMemberStatus(operator.getTenantId(), userId, status);
    }

    @Transactional
    public TenantMemberVO provisionMyEmployee(Long operatorUserId, ProvisionEmployeeRequest request) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        return provisionEmployee(operator.getTenantId(), request);
    }

    @Transactional
    public TenantMemberVO provisionEmployee(Long tenantId, ProvisionEmployeeRequest request) {
        Tenant tenant = requireTenant(tenantId);
        if (!TenantTypes.ENTERPRISE.equals(tenant.getTenantType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅企业租户可开通员工账号");
        }
        String loginName = EnterpriseAccounts.normalizeLoginName(request.account());
        if (tenantRepository.findMemberByLoginName(tenantId, loginName).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该企业内登录账号已存在");
        }
        String email = blankToNull(request.email());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
            if (userRepository.findByEmail(email).isPresent()) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
            }
        }
        quotaApplicationService.assertMemberQuotaAvailable(tenantId);
        User user = new User();
        user.setUsername(uniqueInternalUsername(tenantId, loginName));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        String nickname = blankToNull(request.nickname());
        user.setNickname(nickname == null ? loginName : nickname);
        user.setStatus(1);
        user.setUserType(UserTypes.TENANT_USER);
        userRepository.save(user);
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(user.getId());
        member.setRoleCode(normalizeRoleCode(request.roleCode()));
        member.setLoginName(loginName);
        member.setStatus(1);
        tenantRepository.addMember(member);
        syncToTenantWorkspaces(tenantId, user.getId(), member.getRoleCode());
        return toVo(member);
    }

    @Transactional
    public void resetMyEmployeePassword(Long operatorUserId, Long userId, String password) {
        TenantMember operator = requirePrimaryMember(operatorUserId);
        requireTenantAdmin(operator);
        if (operator.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请在设置中修改自己的密码");
        }
        TenantMember member = tenantRepository.findMember(operator.getTenantId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "租户成员不存在"));
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "成员已停用");
        }
        userRepository.updatePasswordHash(userId, passwordEncoder.encode(password));
    }

    public Optional<TenantMember> findPrimaryMember(Long userId) {
        return tenantRepository.findPrimaryByUserId(userId);
    }

    public Optional<User> findEnterpriseLoginUser(String orgId, String account) {
        String slug = EnterpriseAccounts.normalizeOrgId(orgId);
        Tenant tenant = tenantRepository.findBySlug(slug).orElse(null);
        if (tenant == null
                || !TenantTypes.ENTERPRISE.equals(tenant.getTenantType())
                || tenant.getStatus() == null
                || tenant.getStatus() != 1) {
            return Optional.empty();
        }
        String loginName = EnterpriseAccounts.normalizeLoginName(account);
        TenantMember member = tenantRepository.findMemberByLoginName(tenant.getId(), loginName)
                .orElseGet(() -> {
                    if (!loginName.contains("@")) {
                        return null;
                    }
                    return userRepository.findByEmail(loginName)
                            .flatMap(user -> tenantRepository.findMember(tenant.getId(), user.getId()))
                            .orElse(null);
                });
        if (member == null || member.getStatus() == null || member.getStatus() != 1) {
            return Optional.empty();
        }
        return userRepository.findById(member.getUserId());
    }

    private String uniqueInternalUsername(Long tenantId, String loginName) {
        String base = EnterpriseAccounts.internalUsername(tenantId, loginName);
        if (userRepository.findByUsername(base).isEmpty()) {
            return base;
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
    }

    private TenantMember requirePrimaryMember(Long userId) {
        return tenantRepository.findPrimaryByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "未找到租户"));
    }

    private void requireTenantAdmin(TenantMember member) {
        if (!RoleCodes.TENANT_ADMIN.equals(member.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要租户管理员权限");
        }
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "租户成员已停用");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return RoleCodes.MEMBER;
        }
        if (RoleCodes.TENANT_ADMIN.equals(roleCode) || RoleCodes.MEMBER.equals(roleCode)) {
            return roleCode;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "角色无效");
    }

    private void syncToTenantWorkspaces(Long tenantId, Long userId, String tenantRoleCode) {
        String workspaceRoleCode = RoleCodes.TENANT_ADMIN.equals(tenantRoleCode)
                ? RoleCodes.DEVELOPER
                : RoleCodes.MEMBER;
        for (Workspace workspace : workspaceRepository.listByTenantId(tenantId)) {
            if (workspaceRepository.findMember(workspace.getId(), userId).isPresent()) {
                continue;
            }
            Role role = roleRepository.findByWorkspaceAndCode(workspace.getId(), workspaceRoleCode)
                    .orElse(null);
            if (role == null) {
                continue;
            }
            WorkspaceMember workspaceMember = new WorkspaceMember();
            workspaceMember.setWorkspaceId(workspace.getId());
            workspaceMember.setUserId(userId);
            workspaceMember.setRoleId(role.getId());
            workspaceMember.setStatus(1);
            workspaceRepository.addMember(workspaceMember);
        }
    }

    private TenantMemberVO toVo(TenantMember member) {
        User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return new TenantMemberVO(
                member.getId(),
                member.getUserId(),
                member.getLoginName(),
                user.getEmail(),
                user.getNickname(),
                member.getRoleCode(),
                member.getStatus(),
                member.getJoinedAt());
    }
}
