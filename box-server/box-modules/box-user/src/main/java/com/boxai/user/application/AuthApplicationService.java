package com.boxai.user.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.constant.UserTypes;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.audit.HttpRequestContext;
import com.boxai.security.ratelimit.RateLimitService;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.jwt.JwtService;
import com.boxai.tenant.api.TenantVO;
import com.boxai.tenant.application.TenantApplicationService;
import com.boxai.user.api.AuthVO;
import com.boxai.user.api.ChangePasswordRequest;
import com.boxai.user.api.LoginRequest;
import com.boxai.user.api.RegisterRequest;
import com.boxai.user.api.ResetPasswordRequest;
import com.boxai.user.api.SelectCurrentWorkspaceRequest;
import com.boxai.user.api.SendVerificationCodeRequest;
import com.boxai.user.api.SendVerificationCodeResponse;
import com.boxai.user.api.UpdateProfileRequest;
import com.boxai.user.api.TenantSummaryVO;
import com.boxai.user.api.UserVO;
import com.boxai.user.api.WorkspaceVO;
import com.boxai.user.support.VerificationCodePurpose;
import com.boxai.workspace.application.WorkspaceApplicationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final WorkspaceApplicationService workspaceApplicationService;
    private final TenantApplicationService tenantApplicationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationCodeService verificationCodeService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;
    private final UserPreferenceApplicationService userPreferenceApplicationService;

    public AuthApplicationService(UserRepository userRepository,
                                  WorkspaceApplicationService workspaceApplicationService,
                                  TenantApplicationService tenantApplicationService,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  VerificationCodeService verificationCodeService,
                                  AuditLogService auditLogService,
                                  RateLimitService rateLimitService,
                                  UserPreferenceApplicationService userPreferenceApplicationService) {
        this.userRepository = userRepository;
        this.workspaceApplicationService = workspaceApplicationService;
        this.tenantApplicationService = tenantApplicationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificationCodeService = verificationCodeService;
        this.auditLogService = auditLogService;
        this.rateLimitService = rateLimitService;
        this.userPreferenceApplicationService = userPreferenceApplicationService;
    }

    @Transactional
    public AuthVO register(RegisterRequest request) {
        String accountType = normalizeAccountType(request.accountType());
        if (TenantTypes.ENTERPRISE.equals(accountType)
                && (request.companyName() == null || request.companyName().isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写企业名称");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        verificationCodeService.verify(email, VerificationCodePurpose.REGISTER, request.verificationCode());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }
        String username = email;
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank() ? email.split("@")[0] : request.nickname());
        user.setStatus(1);
        user.setUserType(UserTypes.TENANT_USER);
        userRepository.save(user);
        var tenant = tenantApplicationService.createForRegistration(
                user, accountType, request.companyName(), request.contactEmail());
        var workspace = workspaceApplicationService.createDefaultWorkspace(user, tenant.getId(), tenant.getTenantType());
        userPreferenceApplicationService.setCurrentWorkspaceId(user.getId(), workspace.getId());
        AuthVO auth = issue(user);
        auditLogService.recordForUser(
                user.getId(),
                null,
                AuditActions.REGISTER,
                AuditResourceTypes.AUTH,
                String.valueOf(user.getId()),
                user.getEmail(),
                AuditLogService.RESULT_SUCCESS,
                null);
        return auth;
    }

    public AuthVO login(LoginRequest request) {
        String clientIp = HttpRequestContext.clientIp();
        rateLimitService.assertAllowed("login", clientIp == null ? "unknown" : clientIp, 20, Duration.ofMinutes(1));
        String account = request.account().trim();
        User user = userRepository.findByEmail(account.toLowerCase(Locale.ROOT))
                .or(() -> userRepository.findByUsername(account))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误"));
        if (UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请使用平台管理端登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误");
        }
        String expectedType = normalizeAccountType(request.accountType());
        TenantVO tenant = tenantApplicationService.findPrimaryByUserId(user.getId());
        if (tenant != null && tenant.tenantType() != null && !expectedType.equals(tenant.tenantType())) {
            if (TenantTypes.PERSONAL.equals(expectedType)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "该账号为企业账号，请切换到企业端登录");
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, "该账号为个人账号，请切换到个人端登录");
        }
        userRepository.updateLastLogin(user.getId());
        AuthVO auth = issue(user);
        auditLogService.recordForUser(
                user.getId(),
                null,
                AuditActions.LOGIN,
                AuditResourceTypes.AUTH,
                String.valueOf(user.getId()),
                user.getEmail(),
                AuditLogService.RESULT_SUCCESS,
                null);
        return auth;
    }

    public AuthVO me(LoginUser loginUser) {
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请使用平台管理端登录");
        }
        return issue(user);
    }

    @Transactional
    public AuthVO selectCurrentWorkspace(LoginUser loginUser, SelectCurrentWorkspaceRequest request) {
        workspaceApplicationService.requireAccess(request.workspaceId(), loginUser.userId());
        userPreferenceApplicationService.setCurrentWorkspaceId(loginUser.userId(), request.workspaceId());
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return issue(user);
    }

    @Transactional
    public AuthVO updateProfile(LoginUser loginUser, UpdateProfileRequest request) {
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (request.nickname() == null && request.bio() == null && request.avatarUrl() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少填写一项资料");
        }
        String nickname = request.nickname() == null ? null : request.nickname().trim();
        String bio = request.bio() == null ? null : request.bio().trim();
        String avatarUrl = request.avatarUrl() == null ? null : request.avatarUrl().trim();
        if (nickname != null && nickname.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
        }
        if (avatarUrl != null && avatarUrl.isEmpty()) {
            avatarUrl = "";
        }
        userRepository.updateProfile(user.getId(), nickname, bio, avatarUrl);
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
        }
        return issue(user);
    }

    public SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        VerificationCodePurpose purpose = parsePurpose(request.purpose());
        if (VerificationCodePurpose.REGISTER == purpose && userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }
        if (VerificationCodePurpose.RESET_PASSWORD == purpose
                && userRepository.findByEmail(email).isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册");
        }
        String devCode = verificationCodeService.send(email, purpose);
        return new SendVerificationCodeResponse(devCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册"));
        verificationCodeService.verify(email, VerificationCodePurpose.RESET_PASSWORD, request.verificationCode());
        userRepository.updatePasswordHash(user.getId(), passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void updatePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "原密码错误");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        userRepository.updatePasswordHash(userId, passwordEncoder.encode(request.newPassword()));
    }

    private AuthVO issue(User user) {
        tenantApplicationService.ensurePrimaryTenantActive(user.getId());
        String userType = user.getUserType() == null ? UserTypes.TENANT_USER : user.getUserType();
        String token = jwtService.generate(user.getId(), user.getUsername(), userType);
        List<WorkspaceVO> workspaces = workspaceApplicationService.listMineByUserId(user.getId()).stream()
                .map(item -> new WorkspaceVO(
                        item.id(),
                        item.name(),
                        item.slug(),
                        item.description(),
                        item.avatarUrl(),
                        item.status(),
                        item.roleCode()))
                .toList();
        TenantSummaryVO tenant = toTenantSummary(tenantApplicationService.findPrimaryByUserId(user.getId()));
        Long currentWorkspaceId = resolveCurrentWorkspaceId(user.getId(), workspaces);
        return new AuthVO(
                token,
                new UserVO(user.getId(), user.getUsername(), user.getEmail(), user.getNickname(), user.getAvatarUrl(), user.getBio(), userType),
                tenant,
                workspaces,
                currentWorkspaceId);
    }

    private Long resolveCurrentWorkspaceId(Long userId, List<WorkspaceVO> workspaces) {
        Long saved = userPreferenceApplicationService.getCurrentWorkspaceId(userId);
        boolean valid = saved != null && workspaces.stream().anyMatch(item -> saved.equals(item.id()));
        if (valid) {
            return saved;
        }
        if (workspaces.isEmpty()) {
            return null;
        }
        Long fallback = workspaces.get(0).id();
        userPreferenceApplicationService.setCurrentWorkspaceId(userId, fallback);
        return fallback;
    }

    private TenantSummaryVO toTenantSummary(TenantVO tenant) {
        if (tenant == null) {
            return null;
        }
        return new TenantSummaryVO(tenant.id(), tenant.name(), tenant.slug(), tenant.tenantType());
    }

    private VerificationCodePurpose parsePurpose(String purpose) {
        if (purpose == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码用途无效");
        }
        try {
            return VerificationCodePurpose.valueOf(purpose.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码用途无效");
        }
    }

    private String normalizeAccountType(String accountType) {
        if (accountType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择账号类型");
        }
        if (TenantTypes.PERSONAL.equalsIgnoreCase(accountType)) {
            return TenantTypes.PERSONAL;
        }
        if (TenantTypes.ENTERPRISE.equalsIgnoreCase(accountType)) {
            return TenantTypes.ENTERPRISE;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "账号类型无效");
    }
}
