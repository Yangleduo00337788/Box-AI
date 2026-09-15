package com.boxai.user.application;

import com.boxai.common.constant.PlatformAdminRoles;
import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.result.PageResult;
import com.boxai.domain.plan.TenantUsage;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantMember;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserQuery;
import com.boxai.domain.user.UserRepository;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.user.api.PlatformUserContextVO;
import com.boxai.security.context.LoginUser;
import com.boxai.user.api.CreatePlatformAdminRequest;
import com.boxai.user.api.PlatformUserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AdminPlatformUserApplicationService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TenantUsageRepository tenantUsageRepository;
    private final ExecutionRepository executionRepository;

    public AdminPlatformUserApplicationService(UserRepository userRepository,
                                               PasswordEncoder passwordEncoder,
                                               TenantRepository tenantRepository,
                                               WorkspaceRepository workspaceRepository,
                                               TenantUsageRepository tenantUsageRepository,
                                               ExecutionRepository executionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
        this.tenantUsageRepository = tenantUsageRepository;
        this.executionRepository = executionRepository;
    }

    public PageResult<PlatformUserVO> page(String keyword, String userType, Integer status, int page, int pageSize) {
        UserQuery query = new UserQuery();
        query.setKeyword(trimToNull(keyword));
        query.setUserType(trimToNull(userType));
        query.setStatus(status);
        query.setPage(page);
        query.setPageSize(pageSize);
        PageResult<User> result = userRepository.page(query);
        return new PageResult<>(
                result.records().stream().map(this::toVo).toList(),
                result.total(),
                result.page(),
                result.pageSize());
    }

    @Transactional
    public PlatformUserVO createAdmin(CreatePlatformAdminRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent() || userRepository.findByUsername(email).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱或账号已存在");
        }
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank()
                ? email.split("@")[0]
                : request.nickname().trim());
        user.setStatus(1);
        user.setUserType(UserTypes.PLATFORM_ADMIN);
        user.setPlatformAdminRole(normalizePlatformRole(request.platformAdminRole()));
        userRepository.save(user);
        return toVo(user);
    }

    public PlatformUserContextVO context(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        TenantMember member = tenantRepository.findPrimaryByUserId(userId).orElse(null);
        Tenant tenant = member == null ? null : tenantRepository.findById(member.getTenantId()).orElse(null);
        List<String> workspaceNames = member == null
                ? List.of()
                : workspaceRepository.listByTenantId(member.getTenantId()).stream()
                        .map(Workspace::getName)
                        .toList();
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        TenantUsage usage = tenant == null
                ? null
                : tenantUsageRepository.findByTenantAndPeriod(tenant.getId(), period).orElse(null);
        List<Long> workspaceIds = tenant == null
                ? List.of()
                : workspaceRepository.listByTenantId(tenant.getId()).stream().map(Workspace::getId).toList();
        List<Execution> executions = workspaceIds.isEmpty()
                ? List.of()
                : executionRepository.listRecent(1000).stream()
                        .filter(item -> item.getWorkspaceId() != null && workspaceIds.contains(item.getWorkspaceId()))
                        .toList();
        int failed = (int) executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus().toUpperCase().contains("FAIL"))
                .count();
        double failureRate = executions.isEmpty() ? 0D : failed * 100D / executions.size();
        return new PlatformUserContextVO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                tenant == null ? null : tenant.getName(),
                tenant == null ? null : tenant.getTenantType(),
                workspaceNames,
                usage == null ? 0 : usage.getAiCalls(),
                usage == null ? 0L : usage.getTokens(),
                failureRate);
    }

    @Transactional
    public PlatformUserVO updateStatus(LoginUser operator, Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值无效");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (status == 0 && operator.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能停用当前登录账号");
        }
        if (status == 0 && UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            long activeAdmins = userRepository.countByUserTypeAndStatus(UserTypes.PLATFORM_ADMIN, 1);
            if (user.getStatus() != null && user.getStatus() == 1 && activeAdmins <= 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "至少保留一名启用的平台管理员");
            }
        }
        userRepository.updateStatus(userId, status);
        user.setStatus(status);
        return toVo(user);
    }

    private PlatformUserVO toVo(User user) {
        return new PlatformUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getUserType(),
                user.getPlatformAdminRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePlatformRole(String role) {
        if (role == null || role.isBlank()) {
            return PlatformAdminRoles.OPS;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!PlatformAdminRoles.SUPER_ADMIN.equals(normalized)
                && !PlatformAdminRoles.OPS.equals(normalized)
                && !PlatformAdminRoles.FINANCE.equals(normalized)
                && !PlatformAdminRoles.CONTENT.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "平台角色无效");
        }
        return normalized;
    }
}
