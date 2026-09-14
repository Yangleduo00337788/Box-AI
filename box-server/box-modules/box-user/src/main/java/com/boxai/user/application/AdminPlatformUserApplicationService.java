package com.boxai.user.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.result.PageResult;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserQuery;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.user.api.CreatePlatformAdminRequest;
import com.boxai.user.api.PlatformUserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AdminPlatformUserApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminPlatformUserApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        userRepository.save(user);
        return toVo(user);
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
}
