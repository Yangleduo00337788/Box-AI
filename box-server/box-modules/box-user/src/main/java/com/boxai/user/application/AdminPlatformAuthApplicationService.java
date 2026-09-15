package com.boxai.user.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.user.api.ResetPasswordRequest;
import com.boxai.user.api.SendVerificationCodeRequest;
import com.boxai.user.api.SendVerificationCodeResponse;
import com.boxai.user.support.VerificationCodePurpose;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AdminPlatformAuthApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;

    public AdminPlatformAuthApplicationService(UserRepository userRepository,
                                               PasswordEncoder passwordEncoder,
                                               VerificationCodeService verificationCodeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeService = verificationCodeService;
    }

    public SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "账号不存在"));
        if (!UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该邮箱不是平台管理员");
        }
        String devCode = verificationCodeService.send(email, VerificationCodePurpose.RESET_PASSWORD);
        return new SendVerificationCodeResponse(devCode);
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        verificationCodeService.verify(email, VerificationCodePurpose.RESET_PASSWORD, request.verificationCode());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "账号不存在"));
        if (!UserTypes.PLATFORM_ADMIN.equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该邮箱不是平台管理员");
        }
        userRepository.updatePasswordHash(user.getId(), passwordEncoder.encode(request.newPassword()));
    }
}
