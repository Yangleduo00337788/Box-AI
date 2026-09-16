package com.boxai.tenant.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.jwt.JwtService;
import com.boxai.tenant.api.AdminAuthVO;
import com.boxai.tenant.api.AdminLoginRequest;
import com.boxai.tenant.api.AdminUserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AdminAuthApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthApplicationService(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AdminAuthVO login(AdminLoginRequest request) {
        String account = request.account().trim();
        User user = userRepository.findByEmail(account.toLowerCase(Locale.ROOT))
                .or(() -> userRepository.findByUsername(account))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误"));
        if (!UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理权限");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误");
        }
        userRepository.updateLastLogin(user.getId());
        return issue(user);
    }

    public AdminAuthVO me(LoginUser loginUser) {
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理权限");
        }
        return issue(user);
    }

    private AdminAuthVO issue(User user) {
        String token = jwtService.generate(
                user.getId(), user.getUsername(), user.getUserType(), null, user.getPlatformAdminRole());
        return new AdminAuthVO(token, toUserVo(user));
    }

    private AdminUserVO toUserVo(User user) {
        return new AdminUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getUserType(),
                user.getPlatformAdminRole());
    }
}
