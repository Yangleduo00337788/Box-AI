package com.boxai.user.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.user.api.ResetPasswordRequest;
import com.boxai.user.api.SendVerificationCodeRequest;
import com.boxai.user.support.VerificationCodePurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPlatformAuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationCodeService verificationCodeService;

    @InjectMocks
    private AdminPlatformAuthApplicationService service;

    @Test
    void sendVerificationCodeRejectsNonAdmin() {
        User user = user(UserTypes.TENANT_USER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendVerificationCode(new SendVerificationCodeRequest("User@Example.com", "RESET_PASSWORD")));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        verify(verificationCodeService, never()).send(any(), any());
    }

    @Test
    void sendVerificationCodeReturnsDevCodeForAdmin() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user(UserTypes.PLATFORM_ADMIN)));
        when(verificationCodeService.send("admin@example.com", VerificationCodePurpose.RESET_PASSWORD)).thenReturn("654321");

        var vo = service.sendVerificationCode(new SendVerificationCodeRequest("Admin@Example.com", "RESET_PASSWORD"));

        assertEquals("654321", vo.devCode());
    }

    @Test
    void resetPasswordEncodesNewHash() {
        User user = user(UserTypes.PLATFORM_ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass12")).thenReturn("encoded");

        service.resetPassword(new ResetPasswordRequest("admin@example.com", "123456", "newpass12"));

        verify(verificationCodeService).verify("admin@example.com", VerificationCodePurpose.RESET_PASSWORD, "123456");
        verify(userRepository).updatePasswordHash(8L, "encoded");
    }

    @Test
    void resetPasswordSkipsWhenAccountMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resetPassword(new ResetPasswordRequest("missing@example.com", "123456", "newpass12")));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
        verify(userRepository, never()).updatePasswordHash(anyLong(), any());
    }

    private static User user(String type) {
        User user = new User();
        user.setId(8L);
        user.setUserType(type);
        user.setEmail("admin@example.com");
        return user;
    }
}
