package com.boxai.tenant.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.jwt.JwtService;
import com.boxai.tenant.api.AdminLoginRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AdminAuthApplicationService service;

    @Test
    void loginRejectsTenantUser() {
        User user = user(1L, UserTypes.TENANT_USER, 1);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new AdminLoginRequest("user@example.com", "secret")));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        verify(jwtService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    void loginRejectsDisabledAdmin() {
        User user = user(1L, UserTypes.PLATFORM_ADMIN, 0);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new AdminLoginRequest("admin@example.com", "secret")));
        assertEquals(ErrorCode.USER_DISABLED, ex.getCode());
    }

    @Test
    void loginIssuesTokenForPlatformAdmin() {
        User user = user(8L, UserTypes.PLATFORM_ADMIN, 1);
        user.setPasswordHash("hash");
        user.setPlatformAdminRole("SUPER_ADMIN");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generate(8L, "admin", UserTypes.PLATFORM_ADMIN, null, "SUPER_ADMIN"))
                .thenReturn("admin-jwt");

        var vo = service.login(new AdminLoginRequest("  Admin@Example.com ", "secret"));

        assertEquals("admin-jwt", vo.token());
        assertEquals(8L, vo.user().id());
        assertEquals("SUPER_ADMIN", vo.user().platformAdminRole());
        verify(userRepository).updateLastLogin(8L);
    }

    @Test
    void meRejectsNonAdmin() {
        User user = user(1L, UserTypes.TENANT_USER, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.me(new LoginUser(1L, "user", UserTypes.TENANT_USER)));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    private static User user(Long id, String type, int status) {
        User user = new User();
        user.setId(id);
        user.setUserType(type);
        user.setStatus(status);
        user.setEmail("admin@example.com");
        user.setUsername("admin");
        return user;
    }
}
